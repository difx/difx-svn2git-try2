-- phpMyAdmin SQL Dump
-- version 3.2.4
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Erstellungszeit: 28. Oktober 2011 um 14:10
-- Server Version: 5.1.41
-- PHP-Version: 5.3.1

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Datenbank: `difxdb`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Experiment`
--

DROP TABLE IF EXISTS `Experiment`;
CREATE TABLE IF NOT EXISTS `Experiment` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=11 ;


--
-- Tabellenstruktur für Tabelle `ExperimentAndModule`
--

DROP TABLE IF EXISTS `ExperimentAndModule`;
CREATE TABLE IF NOT EXISTS `ExperimentAndModule` (
  `experimentID` bigint(20) NOT NULL,
  `moduleID` bigint(20) NOT NULL,
  KEY `experimentID` (`experimentID`),
  KEY `moduleID` (`moduleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Job`
--

DROP TABLE IF EXISTS `Job`;
CREATE TABLE IF NOT EXISTS `Job` (
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
) ENGINE=MyISAM DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `Job`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `JobStatus`
--

DROP TABLE IF EXISTS `JobStatus`;
CREATE TABLE IF NOT EXISTS `JobStatus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `status` varchar(100) NOT NULL,
  `active` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=7 ;

--
-- Daten für Tabelle `JobStatus`
--

INSERT INTO `JobStatus` (`id`, `status`, `active`) VALUES
(1, 'unknown', 0),
(2, 'queued', 1),
(3, 'running', 1),
(4, 'complete', 0),
(5, 'killed', 0),
(6, 'failed', 0);

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Module`
--

DROP TABLE IF EXISTS `Module`;
CREATE TABLE IF NOT EXISTS `Module` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `experimentID` bigint(20) unsigned DEFAULT NULL,
  `vsn` varchar(8) NOT NULL,
  `capacity` int(11) NOT NULL,
  `datarate` int(11) NOT NULL,
  `received` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `shipped` timestamp NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `vsn` (`vsn`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=45 ;



--
-- Tabellenstruktur für Tabelle `Pass`
--

DROP TABLE IF EXISTS `Pass`;
CREATE TABLE IF NOT EXISTS `Pass` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `experimentID` bigint(20) unsigned NOT NULL,
  `passName` varchar(30) NOT NULL,
  `passTypeID` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `Pass`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `PassType`
--

DROP TABLE IF EXISTS `PassType`;
CREATE TABLE IF NOT EXISTS `PassType` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `type` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=4 ;

--
-- Daten für Tabelle `PassType`
--

INSERT INTO `PassType` (`id`, `type`) VALUES
(1, 'production'),
(2, 'clock'),
(3, 'test');

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Slot`
--

DROP TABLE IF EXISTS `Slot`;
CREATE TABLE IF NOT EXISTS `Slot` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `moduleID` bigint(20) unsigned DEFAULT NULL,
  `location` varchar(30) NOT NULL,
  `isActive` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `location` (`location`),
  UNIQUE KEY `moduleID` (`moduleID`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=83 ;



-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `vDOIQueue`
--

DROP TABLE IF EXISTS `vDOIQueue`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `difxdb`.`vDOIQueue` AS select `E`.`code` AS `code`,`P`.`passName` AS `passName`,`J`.`jobNumber` AS `jobNumber`,`J`.`priority` AS `priority`,`J`.`jobStart` AS `jobStart`,`J`.`jobDuration` AS `jobDuration`,`J`.`inputFile` AS `inputFile`,`J`.`speedupFactor` AS `speedupFactor`,`J`.`numAntennas` AS `numAntennas`,`S`.`status` AS `status` from ((((`difxdb`.`Job` `J` join `difxdb`.`Pass` `P` on((`J`.`passID` = `P`.`id`))) join `difxdb`.`Experiment` `E` on((`P`.`experimentID` = `E`.`id`))) join `difxdb`.`JobStatus` `S` on((`S`.`id` = `J`.`statusID`))) join `difxdb`.`PassType` on((`difxdb`.`PassType`.`id` = `P`.`passTypeID`)));


--
-- Constraints der Tabelle `Slot`
--
ALTER TABLE `Slot`
  ADD CONSTRAINT `Slot_ibfk_1` FOREIGN KEY (`moduleID`) REFERENCES `Module` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;
