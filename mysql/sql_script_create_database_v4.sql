-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema tracking_system
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema tracking_system
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `tracking_system` DEFAULT CHARACTER SET utf8 ;
USE `tracking_system` ;

-- -----------------------------------------------------
-- Table `tracking_system`.`board`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tracking_system`.`board` (
  `id` INT(5) UNSIGNED NOT NULL AUTO_INCREMENT,
  `coded_id` VARCHAR(50) UNIQUE NOT NULL,
  `datetime` DATETIME NULL,
  PRIMARY KEY (`id`),
  INDEX `datetime` (`datetime` ASC));


-- -----------------------------------------------------
-- Table `tracking_system`.`user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tracking_system`.`user` (
  `id` INT(5) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NULL,
  `surname` VARCHAR(50) NULL,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `password` VARCHAR(60) NOT NULL,
  `admin` TINYINT(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`));


-- -----------------------------------------------------
-- Table `tracking_system`.`machine`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tracking_system`.`machine` (
  `id` INT(5) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) UNIQUE NOT NULL,
  PRIMARY KEY (`id`));


-- -----------------------------------------------------
-- Table `tracking_system`.`test`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tracking_system`.`test` (
  `id` INT(5) UNSIGNED NOT NULL AUTO_INCREMENT,
  `datetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `board` VARCHAR(50) NOT NULL,
  `good` TINYINT(1) NOT NULL,
  `login_id` INT(5) UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `datetime_idx` (`datetime` ASC),
  INDEX `fk_test_board_idx` (`board` ASC),
  INDEX `fk_test_login_idx` (`login_id` ASC),
  CONSTRAINT `fk_test_board`
    FOREIGN KEY (`board`)
    REFERENCES `tracking_system`.`board` (`coded_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_test_login`
    FOREIGN KEY (`login_id`)
    REFERENCES `tracking_system`.`login` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);


-- -----------------------------------------------------
-- Table `tracking_system`.`login`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tracking_system`.`login` (
  `id` INT(5) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `datetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `machine` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_login_user1_idx` (`username` ASC),
  INDEX `datetime_idx` (`datetime` ASC),
  INDEX `fk_login_machine1_idx` (`machine` ASC),
  CONSTRAINT `fk_login_user1`
    FOREIGN KEY (`username`)
    REFERENCES `tracking_system`.`user` (`username`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_login_machine1`
    FOREIGN KEY (`machine`)
    REFERENCES `tracking_system`.`machine` (`name`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

INSERT INTO tracking_system.machine
VALUES (default, 'TRI1');

INSERT INTO tracking_system.machine
VALUES (default, 'TRI2');

-- Creates application MySQL user (used by many apps on the whole system)
CREATE USER 'application'@'%'
IDENTIFIED BY '12Application';
GRANT SELECT ON tracking_system.* TO 'application'@'%';
GRANT INSERT ON tracking_system.* TO 'application'@'%';
GRANT UPDATE ON tracking_system.* TO 'application'@'%';

-- Creates user administrator for particular tasks
-- 		username: admin
--		password: admin
-- Note that password has been salted and hashed by BCrypt algorithm, using default
-- values offered by jBCrypt implementation
INSERT INTO tracking_system.user
VALUES (default, 'Sistema', 'Amministratore', 'admin', '$2a$10$4/oJeMqYlqFEBn/bZs9xK.S5DcQapiOqTrK5.GwrrYW89DEQ93pmy', '1');

CREATE VIEW `yesterday_tests` AS
SELECT * FROM tracking_system.test
WHERE DATE(tracking_system.test.datetime) = DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY);


