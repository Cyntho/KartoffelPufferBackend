-- phpMyAdmin SQL Dump
-- version 5.1.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jul 06, 2022 at 04:31 PM
-- Server version: 10.4.21-MariaDB
-- PHP Version: 8.0.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `kartoffelpuffer`
--

-- --------------------------------------------------------

--
-- Table structure for table `admin_tokens`
--

CREATE TABLE `admin_tokens` (
  `id` int(11) NOT NULL,
  `code` varchar(32) NOT NULL,
  `used_by` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `admin_tokens`
--

INSERT INTO `admin_tokens` (`id`, `code`, `used_by`) VALUES
(3, 'asdfghj', 1),
(4, 'qwertzu', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `allergy_list`
--

CREATE TABLE `allergy_list` (
  `id` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `iconLink` varchar(512) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `allergy_list`
--

INSERT INTO `allergy_list` (`id`, `name`, `iconLink`) VALUES
(1, 'Fisch', '/img/allergies/fish.png'),
(2, 'Soja', '/img/allergies/soja.png'),
(3, 'Gluten', '/img/allergies/gluten.png'),
(4, 'Eier', '/img/allergies/eggs.png'),
(5, 'Nüsse', '/img/allergies/nuts.png'),
(6, 'Lupinen', '/img/allergies/lupinen.png'),
(7, 'Sesam', '/img/allergies/sesam.png'),
(8, 'Milch', '/img/allergies/milk.png');

-- --------------------------------------------------------

--
-- Table structure for table `dishes`
--

CREATE TABLE `dishes` (
  `id` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `iconLink` varchar(512) NOT NULL,
  `description` varchar(1024) NOT NULL,
  `isActive` tinyint(4) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `dishes`
--

INSERT INTO `dishes` (`id`, `name`, `iconLink`, `description`, `isActive`) VALUES
(1, 'Cheeseburger', 'cheeseburger.jpg', 'Ein super leckerer Cheeseburger mit extra Käse', 1),
(2, 'Milchshake Schoko', 'milkshake_chocolate.jpg', 'Milchshake (Schokolade) nach eigenem Rezept', 1),
(3, 'Kartoffelsalat', 'kartoffelsalat.jpg', 'Leckerer Nudelsalat mit viel Majo!', 1),
(4, 'Pizza Diavolo', 'pizza_diavolo.jpg', 'Feurig scharf und immer lecker - Jetzt mit extra viel Käse', 1);

-- --------------------------------------------------------

--
-- Table structure for table `dish_allergies`
--

CREATE TABLE `dish_allergies` (
  `id` int(11) NOT NULL,
  `dish` int(11) NOT NULL,
  `allergy` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `dish_allergies`
--

INSERT INTO `dish_allergies` (`id`, `dish`, `allergy`) VALUES
(1, 2, 1),
(2, 1, 3),
(3, 1, 7),
(4, 3, 4),
(5, 3, 1),
(6, 3, 8),
(7, 2, 8),
(10, 4, 3),
(11, 1, 5);

-- --------------------------------------------------------

--
-- Table structure for table `layouts`
--

CREATE TABLE `layouts` (
  `id` int(11) NOT NULL,
  `size_x` int(11) NOT NULL,
  `size_y` int(11) NOT NULL,
  `data` varchar(4096) NOT NULL,
  `name` varchar(64) NOT NULL,
  `created` timestamp NOT NULL DEFAULT current_timestamp(),
  `validFrom` timestamp NOT NULL DEFAULT current_timestamp(),
  `active` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `layouts`
--

INSERT INTO `layouts` (`id`, `size_x`, `size_y`, `data`, `name`, `created`, `validFrom`, `active`) VALUES
(0, 7, 9, '[1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 2, 2, 0, 2, 2, 1, 1, 0, 0, 0, 0, 0, 1, 1, 2, 2, 0, 2, 0, 1, 1, 0, 0, 0, 2, 0, 1, 1, 2, 2, 0, 2, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1]', 'Some Layout', '2022-07-06 13:46:01', '2022-07-06 13:46:01', 1);

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `id` int(11) NOT NULL,
  `reservation` int(11) NOT NULL,
  `dish` int(11) NOT NULL,
  `amount` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `reservations`
--

CREATE TABLE `reservations` (
  `id` int(11) NOT NULL,
  `layout` int(11) NOT NULL,
  `pos_x` int(11) NOT NULL,
  `pos_y` int(11) NOT NULL,
  `user` int(11) NOT NULL,
  `created` timestamp NOT NULL DEFAULT current_timestamp(),
  `appointment_start` timestamp NOT NULL DEFAULT current_timestamp(),
  `appointment_end` timestamp NOT NULL DEFAULT current_timestamp(),
  `people` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `reservations`
--

INSERT INTO `reservations` (`id`, `layout`, `pos_x`, `pos_y`, `user`, `created`, `appointment_start`, `appointment_end`, `people`) VALUES
(6, 0, 3, 3, 5, '2022-07-06 13:20:51', '2022-07-06 13:00:00', '2022-07-06 18:50:00', 4),
(7, 0, 1, 1, 5, '2022-07-06 13:24:29', '2022-07-06 11:20:00', '2022-07-06 11:55:00', 4),
(8, 0, 5, 2, 5, '2022-07-06 13:47:02', '2022-07-06 13:00:00', '2022-07-06 13:35:00', 4),
(9, 0, 2, 2, 5, '2022-07-06 13:48:04', '2022-07-06 13:05:00', '2022-07-06 13:40:00', 4),
(10, 0, 2, 4, 5, '2022-07-06 13:48:11', '2022-07-06 11:48:00', '2022-07-06 12:23:00', 4);

-- --------------------------------------------------------

--
-- Table structure for table `reservation_dishes`
--

CREATE TABLE `reservation_dishes` (
  `id` int(11) NOT NULL,
  `reservation` int(11) NOT NULL,
  `dish` int(11) NOT NULL,
  `amount` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `reservation_dishes`
--

INSERT INTO `reservation_dishes` (`id`, `reservation`, `dish`, `amount`) VALUES
(6, 6, 1, 3),
(7, 6, 2, 1),
(8, 6, 3, 1),
(9, 6, 4, 1),
(10, 8, 1, 4),
(11, 8, 2, 3),
(12, 8, 3, 1),
(13, 8, 4, 10);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `token` varchar(64) NOT NULL,
  `advertiser` varchar(64) NOT NULL,
  `lastlogin` timestamp NOT NULL DEFAULT current_timestamp(),
  `firstlogin` timestamp NOT NULL DEFAULT current_timestamp(),
  `isAdmin` tinyint(4) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `token`, `advertiser`, `lastlogin`, `firstlogin`, `isAdmin`) VALUES
(1, '60626026-ae84-43d7-9463-3c70ebacb46f', 'e0ece139-62c7-41dd-ba27-21c7e8ca14ad', '2022-07-05 12:09:27', '2022-07-03 16:26:23', 1),
(5, 'ac886224-27d9-4fae-b16e-263ca814d418', '9e5a531e-40b9-4424-b52e-a0e6bfd10182', '2022-07-06 12:12:28', '2022-07-05 14:29:57', 1);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `admin_tokens`
--
ALTER TABLE `admin_tokens`
  ADD PRIMARY KEY (`id`),
  ADD KEY `used_by` (`used_by`);

--
-- Indexes for table `allergy_list`
--
ALTER TABLE `allergy_list`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `dishes`
--
ALTER TABLE `dishes`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `dish_allergies`
--
ALTER TABLE `dish_allergies`
  ADD PRIMARY KEY (`id`),
  ADD KEY `dish` (`dish`),
  ADD KEY `allergy` (`allergy`);

--
-- Indexes for table `layouts`
--
ALTER TABLE `layouts`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `reservations`
--
ALTER TABLE `reservations`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `reservation_dishes`
--
ALTER TABLE `reservation_dishes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `reservation_dishes_ibfk_1` (`dish`),
  ADD KEY `reservation_dishes_ibfk_2` (`reservation`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `admin_tokens`
--
ALTER TABLE `admin_tokens`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `allergy_list`
--
ALTER TABLE `allergy_list`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `dishes`
--
ALTER TABLE `dishes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `dish_allergies`
--
ALTER TABLE `dish_allergies`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `layouts`
--
ALTER TABLE `layouts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `reservations`
--
ALTER TABLE `reservations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `reservation_dishes`
--
ALTER TABLE `reservation_dishes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `admin_tokens`
--
ALTER TABLE `admin_tokens`
  ADD CONSTRAINT `admin_tokens_ibfk_1` FOREIGN KEY (`used_by`) REFERENCES `users` (`id`);

--
-- Constraints for table `dish_allergies`
--
ALTER TABLE `dish_allergies`
  ADD CONSTRAINT `dish_allergies_ibfk_1` FOREIGN KEY (`dish`) REFERENCES `dishes` (`id`),
  ADD CONSTRAINT `dish_allergies_ibfk_2` FOREIGN KEY (`allergy`) REFERENCES `allergy_list` (`id`);

--
-- Constraints for table `reservation_dishes`
--
ALTER TABLE `reservation_dishes`
  ADD CONSTRAINT `reservation_dishes_ibfk_1` FOREIGN KEY (`dish`) REFERENCES `dishes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `reservation_dishes_ibfk_2` FOREIGN KEY (`reservation`) REFERENCES `reservations` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
