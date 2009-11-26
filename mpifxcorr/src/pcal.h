#ifndef _PCAL_H
#define _PCAL_H
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
#include <cstddef>
#include <stdint.h>
using std::size_t;

class PCalExtractorTrivial;
class PCalExtractorShifting;
class PCalExtractorImplicitShift;
class pcal_config_pimpl;

/////////////////////////////////////////////////////////////////////////////////////////////////////////
// BASE CLASS : Factory method
/////////////////////////////////////////////////////////////////////////////////////////////////////////

class PCal {

   public:
      PCal() {};
      ~PCal() {};

   public:
      /**
       * Factory that returns a new PCal extractor object. The optimal implementation
       * is selected based on the input parameters.
       * @param bandwidth_hz     Bandwidth of the input signal in Hertz
       * @param pcal_spacing_hz  Spacing of the PCal signal, comb spacing, typically 1e6 Hertz
       * @param pcal_offset_hz   Offset of the first PCal signal from 0Hz/DC, typically 10e3 Hertz
       * @return new PCal extractor class instance 
       */
      static PCal* getNew(double bandwidth_hz, double pcal_spacing_hz, double pcal_offset_hz);

   public:
      /**
       * Set the extracted and accumulated PCal data back to zero.
       */
      virtual void clear() = 0;
      
      /**
       * Like clear() but with start time of the data as argument.
       */
      virtual void reset(const long int t) = 0;

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
       * @param samples Chunk of the input signal consisting of 'float' samples
       * @param len     Length of the input signal chunk
       * @return true on success
       */
      virtual bool extractAndIntegrate(f32 const* samples, const size_t len) = 0;

      /**
       * Returns the length in data-seconds of the currently integrated PCal results.
       * The seconds value is derived from sample count and input bandwidth.
       * @return current integration time in seconds
       */
      double getSeconds() { return ((_fs_hz==0.0f) ? 0.0f : (double(_samplecount)/_fs_hz)); }

      /**
       * Get length of vector the user should reserve for getFinalPCal() output copy.
       * @return vector length in complex samples
       */
      int getLength() { return _N_bins; /*TODO: add FFT, copy just _N_tones*/ }

      /**
       * Performs finalization steps on the internal PCal results if necessary
       * and then copies these PCal results into the specified output array.
       * Data in the output array is overwritten with PCal results.
       *
       * @param pointer to user PCal array with getLength() values
       */
      virtual void getFinalPCal(cf32* out) = 0;

      /**
       * Processes samples and accumulates the detected phase calibration tone vector.
       * Computation uses the slowest thinkable direct method. This function is
       * intended for testing and comparison only!
       * @param  data          pointer to input sample vector
       * @param  len           length of input vector
       * @param  pcalout       output array of sufficient size to store extracted PCal
       */
      bool extractAndIntegrate_reference(f32 const* data, const size_t len, cf32* pcalout);

   private:
     /**
      * Return greatest common divisor.
      */
      static long long gcd(long long, long long);

   private:
      uint64_t _samplecount;
      double _fs_hz;
      double _pcaloffset_hz;
      int _N_bins;
      int _N_tones;
      bool _finalized;
      
      long int starttime;
 
      pcal_config_pimpl* _cfg;

   friend class PCalExtractorTrivial;
   friend class PCalExtractorShifting;
   friend class PCalExtractorImplicitShift;
};

#endif // _PCAL_H
