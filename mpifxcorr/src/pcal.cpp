/********************************************************************************************************
 * @file PCal.h
 * Multi-tone Phase Cal Extraction
 *
 * @brief Extracts and integrates multi-tone phase calibration signal information from an input signal.
 *
 * The principle relies on the fact that with a comb spacing of say 1 MHz and a sampling rate of say
 * 32 MHz the single 1 MHz and also all of its multiples (1, 2, .. 16 MHz in the band) have at least
 * one full sine period in 32MHz/1MHz = 32 samples. For extraction and time integration, we simply
 * have to segment the input signal into 32-sample pieces (in the above example) and integrate these
 * pieces.
 *
 * A tiny FFT performed on the integrated 32-bin result gives you the amplitude and phase
 * of every tone. As the PCal amplitude is in practice constant over a short frequency band,
 * the amplitude and phase info after the FFT directly gives you the equipment filter response.
 *
 * The extracted PCal can also be analyzed in the time domain (no FFT). The relative, average instrumental
 * delay time can be found directly by estimating the position of the peak in the time-domain data.
 * 
 * @author   Jan Wagner
 * @author   Sergei Pogrebenko
 * @author   Walter Brisken
 * @version  1.1/2009
 * @license  GNU GPL v3
 *
 * Changelog:
 *   05Oct2009 - added support for arbitrary input segment lengths
 *   08oct2009 - added Briskens rotationless method
 *
 ********************************************************************************************************/

#include "architecture.h"
#include "pcal.h"
#include "pcal_impl.h"
#include <iostream>
#include <cmath>
using std::cerr;
using std::endl;

#include <malloc.h>    // memalign
#define UNROLL_BY_4(x) { x }{ x }{ x }{ x }
#define VALIGN __attribute__((aligned(16)))

class pcal_config_pimpl {
  public:
    pcal_config_pimpl()  {};
    ~pcal_config_pimpl() {};
  public:
    double   dphi;
    cf32* rotator;        // pre-cooked oscillator values
    cf32* rotated;        // temporary
    cf32* pcal_complex;   // temporary unassembled output, later final output
    f32*  pcal_real;      // temporary unassembled output for the pcaloffsethz==0.0f case
    size_t   rotatorlen;
    size_t   pcal_index;     // zero, changes when extract() is called at least once with "leftover" samples
    size_t   rotator_index;  // zero, changes when extract() is called at least once with "leftover" samples
};


/////////////////////////////////////////////////////////////////////////////////////////////////////////
// BASE CLASS: factory and helpers
/////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Factory that returns a new PCal extractor object. The optimal implementation
 * is selected based on the input parameters.
 * @param bandwidth_hz     Bandwidth of the input signal in Hertz
 * @param pcal_spacing_hz  Spacing of the PCal signal, comb spacing, typically 1e6 Hertz
 * @param pcal_offset_hz   Offset of the first PCal signal from 0Hz/DC, typically 10e3 Hertz
 * @return new PCal extractor class instance
 */
PCal* PCal::getNew(double bandwidth_hz, double pcal_spacing_hz, double pcal_offset_hz) 
{
    if (pcal_offset_hz == 0.0f) {
        return new PCalExtractorTrivial(bandwidth_hz, pcal_spacing_hz);
    }
    // if ( __unlikely ((2*bandwidth_hz / gcd(2*bandwidth_hz,pcal_spacing_hz)) > someLengthLimit) ) {
    //    use oscillator implementation instead
    // } else {
    int No, Np;
    No = 2*bandwidth_hz / gcd(pcal_offset_hz, 2*bandwidth_hz);
    Np = 2*bandwidth_hz / gcd(pcal_spacing_hz, 2*bandwidth_hz);
    if ((No % Np) == 0) {
        return new PCalExtractorImplicitShift(bandwidth_hz, pcal_spacing_hz, pcal_offset_hz);
    }
    return new PCalExtractorShifting(bandwidth_hz, pcal_spacing_hz, pcal_offset_hz);
}

/**
 * Greatest common divisor.
 */
long long PCal::gcd(long long a, long long b)
{
    while (true) {
        a = a%b;
        if (a == 0) {
           return b;
        }
        b = b%a;
        if (b == 0) {
           return a;
        }
    }
}

/**
 * Processes samples and accumulates the detected phase calibration tone vector.
 * Computation uses the slowest thinkable direct method. This function is 
 * intended for testing and comparison only!
 * @param  data          pointer to input sample vector
 * @param  len           length of input vector
 * @param  pcalout       output array of sufficient size to store extracted PCal
 */
bool PCal::extractAndIntegrate_reference(f32 const* data, const size_t len, cf32* pcalout)
{
    double dphi = 2*M_PI * (-_pcaloffset_hz/_fs_hz);
    for (size_t n=0; n<len; n++) {
        int bin = (n % _N_bins);
        pcalout[bin].re += cos(dphi * (_samplecount + n)) * data[n];
        pcalout[bin].im += sin(dphi * (_samplecount + n)) * data[n];
    }
    _samplecount += len;
    return true;
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////
// DERIVED CLASS: extraction of zero-offset PCal signals
/////////////////////////////////////////////////////////////////////////////////////////////////////////

PCalExtractorTrivial::PCalExtractorTrivial(double bandwidth_hz, double pcal_spacing_hz)
{
    /* Derive config */
    _cfg = new pcal_config_pimpl();
    _fs_hz   = 2*bandwidth_hz;
    _N_bins  = _fs_hz / gcd(std::abs(pcal_spacing_hz), _fs_hz);
    _N_tones = std::floor(bandwidth_hz / pcal_spacing_hz);
    _pcaloffset_hz = 0.0f;

    /* Allocate */
    _cfg->pcal_complex = (cf32*)memalign(128, sizeof(cf32) * _N_bins * 2);
    _cfg->pcal_real    = (f32*)memalign(128, sizeof(f32) * _N_bins * 2);
    this->clear();
}

PCalExtractorTrivial::~PCalExtractorTrivial()
{
    free(_cfg->pcal_complex);
    free(_cfg->pcal_real);
    delete _cfg;
}

/**
 * Set the extracted and accumulated PCal data back to zero.
 */
void PCalExtractorTrivial::clear()
{
    _samplecount = 0;
    _finalized   = false;
    vectorZero_cf32(_cfg->pcal_complex, _N_bins * 2);
    vectorZero_f32 (_cfg->pcal_real,    _N_bins * 2);
    _cfg->rotator_index = 0;
    _cfg->pcal_index    = 0;
}

void reset(const long int t)
{
  clear();
  starttime = t;
}

/**
 * Extracts multi-tone PCal information from a single-channel signal segment
 * and integrates it to the class-internal PCal extraction result buffer.
 * There are no restrictions to the segment length.
 *
 * If you integrate over a longer time and several segments, i.e. perform
 * multiple calls to this function, take care to keep the input
 * continuous (i.e. don't leave out samples).
 *
 * If extraction has been finalized by calling getFinalPCal() this function
 * returns False. You need to call clear() to reset.
 *
 * @paran samples Chunk of the input signal consisting of 'float' samples
 * @param len     Length of the input signal chunk
 * @return true on success
 */
bool PCalExtractorTrivial::extractAndIntegrate(f32 const* samples, const size_t len)
{
    if (_finalized) { return false; }

    f32 const* src = samples;
    f32* dst = &(_cfg->pcal_real[_cfg->pcal_index]);
    size_t tail = (len % _N_bins);
    size_t end  = len - tail;

    /* Process the first part that fits perfectly */
    for (size_t n = 0; n < end; n+=_N_bins, src+=_N_bins) {
        vectorAdd_f32_I(src, dst, _N_bins);
    }

    /* Handle any samples that didn't fit */
    if (tail != 0) {
        vectorAdd_f32_I(src, dst, tail);
        _cfg->pcal_index = (_cfg->pcal_index + tail) % _N_bins;
    }

    /* Done! */
    _samplecount += len;
    return true;
}

/**
 * Performs finalization steps on the internal PCal results if necessary
 * and then copies these PCal results into the specified output array.
 * Data in the output array is overwritten with PCal results.
 *
 * @param pointer to user PCal array with getLength() values
 */
void PCalExtractorTrivial::getFinalPCal(cf32* out)
{
    IppsDFTSpec_C_32fc** pDFTSpec;
    Ipp8u* pBuffer;
  
    if (!_finalized) {
        _finalized = true;
        vectorAdd_f32_I(/*src*/&(_cfg->pcal_real[_N_bins]), /*srcdst*/&(_cfg->pcal_real[0]), _N_bins);
        vectorRealToComplex_f32(/*srcRe*/_cfg->pcal_real, /*srcIm*/NULL, _cfg->pcal_complex, _N_bins);
    }
    
    // This is substituted by DFT
    // vectorCopy_cf32(/*src*/_cfg->pcal_complex, /*dst*/out, _N_bins);
    
    // Discrete Fourier Transform of the output
    
    // Initialization of the Fourier Transform.
    // IPP_FFT_NODIV_BY_ANY means that no normalization of the spectrum is done.
    // ippAlgHintFast indicates that the fast algorithm is used for the DFT.
    ippsDFTInitAlloc_C_32f(pDFTSpec, _N_bins, 1, IPP_FFT_NODIV_BY_ANY, ippAlgHintFast);
    
    // Perform the DFT for complex data type
    ippsDFTFwd_CToC_32fc(_cfg->pcal_complex, out, pDFTSpec, pBuffer);    
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////
// DERIVED CLASS: extraction of PCal signals with non-zero offset
/////////////////////////////////////////////////////////////////////////////////////////////////////////

PCalExtractorShifting::PCalExtractorShifting(double bandwidth_hz, double pcal_spacing_hz, double pcal_offset_hz)
{
    /* Derive config */
    _fs_hz         = 2 * bandwidth_hz;
    _N_bins        = _fs_hz / gcd(std::abs(pcal_spacing_hz), _fs_hz);
    _N_tones       = std::floor((bandwidth_hz - pcal_offset_hz) / pcal_spacing_hz) + 1;
    _pcaloffset_hz = pcal_offset_hz;
    _cfg = new pcal_config_pimpl();
    _cfg->rotatorlen = _fs_hz / gcd(std::abs(_pcaloffset_hz), _fs_hz);

    /* Allocate */
    _cfg->pcal_complex = (cf32*)memalign(128, sizeof(cf32) * _N_bins * 2);
    _cfg->pcal_real    = (f32*)memalign(128, sizeof(f32) * _N_bins * 2);
    _cfg->rotator = (cf32*)memalign(128, sizeof(cf32) * _cfg->rotatorlen * 2);
    _cfg->rotated = (cf32*)memalign(128, sizeof(cf32) * _cfg->rotatorlen * 2);
    this->clear();

    /* Prepare frequency shifter/mixer lookup */
    _cfg->dphi = 2*M_PI * (_pcaloffset_hz/_fs_hz);
    for (size_t n = 0; n < (2 * _cfg->rotatorlen); n++) {
        double arg = _cfg->dphi * double(n);
        _cfg->rotator[n].re = f32(cos(arg));
        _cfg->rotator[n].im = f32(sin(arg));
    }
}

PCalExtractorShifting::~PCalExtractorShifting()
{
    free(_cfg->pcal_complex);
    free(_cfg->pcal_real);
    free(_cfg->rotator);
    free(_cfg->rotated);
    delete _cfg;
}

/**
 * Set the extracted and accumulated PCal data back to zero.
 */
void PCalExtractorShifting::clear()
{
    _samplecount = 0;
    _finalized   = false;
    vectorZero_cf32(_cfg->pcal_complex, _N_bins * 2);
    vectorZero_f32 (_cfg->pcal_real,    _N_bins * 2);
    vectorZero_cf32(_cfg->rotated,      _cfg->rotatorlen * 2);
    _cfg->rotator_index = 0;
    _cfg->pcal_index    = 0;
}

/**
 * Extracts multi-tone PCal information from a single-channel signal segment
 * and integrates it to the class-internal PCal extraction result buffer.
 * There are no restrictions to the segment length.
 *
 * If you integrate over a longer time and several segments, i.e. perform
 * multiple calls to this function, take care to keep the input
 * continuous (i.e. don't leave out samples).
 *
 * If extraction has been finalized by calling getFinalPCal() this function
 * returns False. You need to call clear() to reset.
 *
 * @paran samples Chunk of the input signal consisting of 'float' samples
 * @param len     Length of the input signal chunk
 * @return true on success
 */
bool PCalExtractorShifting::extractAndIntegrate(f32 const* samples, const size_t len)
{
    if (_finalized) { return false; }

    f32 const* src = samples;
    cf32* dst = &(_cfg->pcal_complex[_cfg->pcal_index]);
    size_t tail  = (len % _cfg->rotatorlen);
    size_t end   = len - tail;

    /* This method is only marginally different from the PCalExtractorTrivial method.
     * Because now our multi-tone PCal signal tones do not reside at integer MHz frequencies,
     * or rather, not at integer multiples of the tone spacing of the comb, the first PCal
     * tone is found at some offset '_pcaloffset_hz' away from 0Hz/DC. 
     * So we just use a complex oscillator to shift the input signal back into place.
     * The complex oscillator has a period of _fs_hz/gcd(_fs_hz,_pcaloffset_hz).
     * The period is usually very short, say, 1600 samples. We precomputed those
     * in the constructor and use them here.
     */

    /* Process the first part that fits perfectly (and note: rotatorlen modulo _N_bins is 0!) */
    for (size_t n = 0; n < end; n+=_cfg->rotatorlen, src+=_cfg->rotatorlen) {
        vectorMul_f32cf32(/*A*/src, 
                        /*B*/&(_cfg->rotator[_cfg->rotator_index]), 
                        /*dst*/&(_cfg->rotated[_cfg->rotator_index]), 
                        /*len*/_cfg->rotatorlen);
        cf32* pulse = &(_cfg->rotated[_cfg->rotator_index]);
        for (size_t p = 0; p < (_cfg->rotatorlen/_N_bins); p++) {
            vectorAdd_cf32_I(/*src*/pulse, /*srcdst*/dst, _N_bins);
            pulse += _N_bins;
        }
    }

    /* Handle any samples that didn't fit */
    if (tail != 0) {
        vectorMul_f32cf32(
            /*A*/src, 
            /*B*/&(_cfg->rotator[_cfg->rotator_index]),
            /*dst*/&(_cfg->rotated[_cfg->rotator_index]), 
            /*len*/tail);
        cf32* pulse = &(_cfg->rotated[_cfg->rotator_index]);
        size_t tail2 = (tail % _N_bins);
        size_t end2  = tail - tail2;
        for (size_t p = 0; p < (end2/_N_bins); p++) {
            vectorAdd_cf32_I(/*src*/pulse, /*srcdst*/dst, _N_bins);
            pulse += _N_bins;
        }
        /* Samples that didn't fit _N_bins */
        vectorAdd_cf32_I(/*src*/pulse, /*srcdst*/dst, tail2);
        _cfg->rotator_index = (_cfg->rotator_index + tail) % _cfg->rotatorlen;
        _cfg->pcal_index    = (_cfg->pcal_index + tail2)   % _N_bins;
    }

    /* Done! */
    _samplecount += len;
    return true;
}

/**
 * Performs finalization steps on the internal PCal results if necessary
 * and then copies these PCal results into the specified output array.
 * Data in the output array is overwritten with PCal results.
 *
 * @param pointer to user PCal array with getLength() values
 */
void PCalExtractorShifting::getFinalPCal(cf32* out)
{
    if (!_finalized) {
        _finalized = true;
         vectorAdd_cf32_I(/*src*/&(_cfg->pcal_complex[_N_bins]), /*srcdst*/&(_cfg->pcal_complex[0]), _N_bins);
    }
    vectorCopy_cf32(/*src*/_cfg->pcal_complex, /*dst*/out, _N_bins);
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////
// DERIVED CLASS: extraction of PCal signals with non-zero offset and FFT-implicit rotation possible
/////////////////////////////////////////////////////////////////////////////////////////////////////////

PCalExtractorImplicitShift::PCalExtractorImplicitShift(double bandwidth_hz, double pcal_spacing_hz, double pcal_offset_hz)
{
    /* Derive config */
    _fs_hz         = 2 * bandwidth_hz;
    _N_bins        = _fs_hz / gcd(std::abs(_pcaloffset_hz), _fs_hz);
    _N_tones       = std::floor((bandwidth_hz - pcal_offset_hz) / pcal_spacing_hz) + 1;
    _pcaloffset_hz = pcal_offset_hz;
    _cfg = new pcal_config_pimpl();

    /* Allocate */
    _cfg->pcal_complex = (cf32*)memalign(128, sizeof(cf32) * _N_tones);
    _cfg->pcal_real    = (f32*) memalign(128, sizeof(f32) * _N_bins * 2);
    this->clear();
}

PCalExtractorImplicitShift::~PCalExtractorImplicitShift()
{
    free(_cfg->pcal_complex);
    free(_cfg->pcal_real);
    delete _cfg;
}

/**
 * Set the extracted and accumulated PCal data back to zero.
 */
void PCalExtractorImplicitShift::clear()
{
    _samplecount = 0;
    _finalized   = false;
    vectorZero_cf32(_cfg->pcal_complex, _N_tones);
    vectorZero_f32 (_cfg->pcal_real,    _N_bins * 2);
    _cfg->pcal_index = 0;
}

/**
 * Extracts multi-tone PCal information from a single-channel signal segment
 * and integrates it to the class-internal PCal extraction result buffer.
 * There are no restrictions to the segment length.
 *
 * If you integrate over a longer time and several segments, i.e. perform
 * multiple calls to this function, take care to keep the input
 * continuous (i.e. don't leave out samples).
 *
 * If extraction has been finalized by calling getFinalPCal() this function
 * returns False. You need to call clear() to reset.
 *
 * @paran samples Chunk of the input signal consisting of 'float' samples
 * @param len     Length of the input signal chunk
 * @return true on success
 */
bool PCalExtractorImplicitShift::extractAndIntegrate(f32 const* samples, const size_t len)
{
    if (_finalized) { return false; }

    f32 const* src = samples;
    f32* dst = &(_cfg->pcal_real[_cfg->pcal_index]);
    size_t tail = (len % _N_bins);
    size_t end  = len - tail;

    /* This method is from Walter Brisken, it works perfectly for smallish 'len'
     * and when offset and tone spacing have suitable properties.
     * Instead of rotating the input to counteract the offset, we bin
     * into a long vector with size of the offset repeat length (again *2 to avoid
     * buffer wraps). After long-term integration, we copy desired FFT bins
     * into PCal. The time-domain PCal can be derived from inverse FFT.
     */

    /* Process the first part that fits perfectly */
    for (size_t n = 0; n < end; n+=_N_bins, src+=_N_bins) {
        vectorAdd_f32_I(src, /*srcdst*/dst, _N_bins);
    }

    /* Handle any samples that didn't fit */
    if (tail != 0) {
        vectorAdd_f32_I(src, /*srcdst*/dst, tail);
        _cfg->pcal_index = (_cfg->pcal_index + tail) % _N_bins;
    }

    /* Done! */
    _samplecount += len;
    return true;
}

/**
 * Performs finalization steps on the internal PCal results if necessary
 * and then copies these PCal results into the specified output array.
 * Data in the output array is overwritten with PCal results.
 *
 * @param pointer to user PCal array with getLength() values
 */
void PCalExtractorImplicitShift::getFinalPCal(cf32* out)
{
    /* TODO: perform FFT/DFT, copy the tone bins.
     * The bins are at 
     *    idx = (_N_bins*f_offset_hz/fs_hz) + 0..Ntones-1 * (_N_bins*f_spacing_hz/fs_hz) - 1
     */
    if (!_finalized) {
        _finalized = true;
         vectorAdd_cf32_I(/*src*/&(_cfg->pcal_complex[_N_bins]), /*srcdst*/&(_cfg->pcal_complex[0]), _N_bins);
    }
    vectorCopy_cf32(/*src*/_cfg->pcal_complex, /*dst*/out, _N_bins);
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////
// FOR FUTURE REFERENCE
/////////////////////////////////////////////////////////////////////////////////////////////////////////

#if 0
/**
 * Process samples and accumulate the detected phase calibration tone vector.
 * Accumulation takes place into the internal (ipp32fc*)pcalcfg.pcal.
 * @param  data    pointer to input sample vector
 * @param  len     length of input vector
 */
void PCal::extract_continuous(f32* data, size_t len)
{
    /* Works on almost the same principle as extract(), but
     * instead of a precomputed pcalcfg.rotatevec[], we use
     * SSE/MMX C instrinsics to compute the phase angles
     * online in registers, faster than cache access!
     */

    // Oscillator: x[1] = 2*cos(phi) * x[0] - x[-1]
    // Rewritten as two oscillators with step 2phi and offset +0,phi
    //    x1[1] = 2*cos(2phi) * x1[0] - x1[-1]
    //    x2[1] = 2*cos(2phi) * x2[0] - x2[-1]
    // Complex data layout:
    //    vec128bit = vec4float = [cos((n-1)*phi) ; sin((n-1)*phi) ; cos(n*phi)  ; sin(n*phi)]
    //    multiplicand          * [2*cos(2phi)    ; 2*cos(2phi)    ; 2*cos(2phi) ; 2*cos(2phi)]

    static __m128 mmxOldComplex_1, mmxOldComplex_0, mmxMult;

    if (sample_count == 0) {
//    if (1) {
        float dphi = pcalcfg.phase_inc;
        float p0 = sample_count * dphi;
        mmxOldComplex_0 = _mm_set_ps( sinf(p0+1*dphi), cosf(p0+1*dphi), 
                                      sinf(p0+0*dphi), cosf(p0+0*dphi) );
        mmxOldComplex_1 = _mm_set_ps( sinf(p0+3*dphi), cosf(p0+3*dphi), 
                                      sinf(p0+2*dphi), cosf(p0+2*dphi) );
        mmxMult = _mm_set1_ps( 2*cosf(2*dphi) );
    }

    float *accu = (float*)&pcalcfg.pcal[0];
    float *data_end = data + len;
    //cerr << " data=" << data << " data_end=" << data_end << " diff=" << (data_end-data) << " len=" << len << endl;
    //while (data < data_end) {
    for (size_t xxx=0; xxx<len; xxx+=pcalcfg.tonebins) {
        __m128 mmxData, mmxPcal, mmxTmp, mmxLoad;
        for (int bin=0; bin<int(pcalcfg.tonebins); bin+=4, data+=4, sample_count+=4) {
            /* rotate and bin first 2 samples */
            mmxLoad = _mm_load_ps(data);  // _mm_loadu_ps() if unaligned
            mmxData = _mm_unpacklo_ps(mmxLoad, mmxLoad);
            mmxPcal = _mm_load_ps(&accu[2*bin+0]);
            mmxData = _mm_mul_ps(mmxData, mmxOldComplex_0);
            mmxPcal = _mm_add_ps(mmxPcal, mmxData);
            _mm_store_ps(&accu[2*bin+0], mmxPcal);

//_mm_store_ps(v4tmp, mmxOldComplex_1);
//printf(" %f %f %f %f\n", v4tmp[0], v4tmp[1], v4tmp[2], v4tmp[3]);

            /* rotate and bin next 2 samples */
            mmxData = _mm_unpackhi_ps(mmxLoad, mmxLoad);
            mmxPcal = _mm_load_ps(&accu[2*bin+4]);
            mmxData = _mm_mul_ps(mmxData, mmxOldComplex_1);
            mmxPcal = _mm_add_ps(mmxPcal, mmxData);
            _mm_store_ps(&accu[2*bin+4], mmxPcal);
            /* compute next four complex value pairs */
            mmxTmp = _mm_mul_ps(mmxMult, mmxOldComplex_1);
            mmxTmp = _mm_sub_ps(mmxTmp, mmxOldComplex_0);
            mmxOldComplex_0 = mmxOldComplex_1;
            mmxOldComplex_1 = mmxTmp;
            mmxTmp = _mm_mul_ps(mmxMult, mmxOldComplex_1);
            mmxTmp = _mm_sub_ps(mmxTmp, mmxOldComplex_0);
            mmxOldComplex_0 = mmxOldComplex_1;
            mmxOldComplex_1 = mmxTmp;
         }
   }

//    sample_count += len;
    return;
}


/**
 * Process samples and accumulate the detected phase calibration tone vector.
 * Accumulation takes place into the internal (ipp32fc*)pcalcfg.pcal.
 * Computation uses the slowest thinkable direct method.
 * @param  data    pointer to input sample vector
 * @param  len     length of input vector
 */
void PCal::extract_analytic(f32* data, size_t len)
{
    for (size_t n=0; n<len; n++) {
        int bin = (n % pcalcfg.tonebins);
        pcalcfg.pcal[bin].re += cosf(pcalcfg.phase_inc * (sample_count + n)) * data[n];
        pcalcfg.pcal[bin].im += sinf(pcalcfg.phase_inc * (sample_count + n)) * data[n];
    }
    sample_count += len;
}
#endif
