-- MySQL Administrator dump 1.4
--
-- ------------------------------------------------------
-- Server version	5.1.49-log


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


--
-- Create schema difxdb
--

CREATE DATABASE IF NOT EXISTS difxdb;
USE difxdb;

--
-- Definition of table `difxdb`.`Experiment`
--
CREATE TABLE  `difxdb`.`Experiment` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(20) CHARACTER SET latin1 NOT NULL,
  `segment` varchar(2) CHARACTER SET latin1 DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=166 DEFAULT CHARSET=utf8;

--
-- Definition of table `difxdb`.`Job`
--
CREATE TABLE  `difxdb`.`Job` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `passID` bigint(20) unsigned NOT NULL,
  `jobNumber` int(11) NOT NULL,
  `queueTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `priority` int(11) NOT NULL DEFAULT '0',
  `correlationStart` timestamp NULL DEFAULT NULL,
  `correlationEnd` timestamp NULL DEFAULT NULL,
  `jobStart` float NOT NULL,
  `jobDuration` float NOT NULL,
  `inputFile` varchar(255) NOT NULL,
  `outputFile` varchar(255) DEFAULT NULL,
  `outputSize` bigint(20) DEFAULT NULL,
  `difxVersion` varchar(30) NOT NULL,
  `speedupFactor` float DEFAULT NULL,
  `numAntennas` int(11) NOT NULL,
  `numForeign` int(11) NOT NULL,
  `dutyCycle` float DEFAULT NULL,
  `statusID` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=2536 DEFAULT CHARSET=utf8;

--
-- Definition of table `difxdb`.`JobStatus`
--
CREATE TABLE  `difxdb`.`JobStatus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `status` varchar(100) NOT NULL,
  `active` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `difxdb`.`JobStatus`
--
INSERT INTO `difxdb`.`JobStatus` VALUES  (1,'unknown',0),
 (2,'queued',1),
 (3,'running',1),
 (4,'complete',0),
 (5,'killed',0),
 (6,'failed',0);

--
-- Definition of table `difxdb`.`Pass`
--
CREATE TABLE  `difxdb`.`Pass` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `experimentID` bigint(20) unsigned NOT NULL,
  `passName` varchar(30) NOT NULL,
  `passTypeID` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=71 DEFAULT CHARSET=utf8;

--
-- Definition of table `difxdb`.`PassType`
--
CREATE TABLE  `difxdb`.`PassType` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `type` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `difxdb`.`PassType`
--
INSERT INTO `difxdb`.`PassType` VALUES  (1,'production'),
 (2,'clock'),
 (3,'test');



/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
