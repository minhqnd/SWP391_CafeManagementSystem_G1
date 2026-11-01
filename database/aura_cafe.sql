-- =====================================================
-- AURA CAFE ERP - CONSOLIDATED DATABASE
-- Canonical MySQL 8.x schema and data snapshot (24 tables)
-- Generated from aura_cafe_erp on 2026-10-28
-- =====================================================
-- WARNING: Running this entire file resets tables and reloads sample data.
-- For an existing database, run ONLY the following optional upgrade block
-- (without the enclosing comment markers). It preserves existing rows.
/*
USE aura_cafe_erp;
SET @ingredient_active_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ingredients' AND column_name = 'is_active'),
    'SELECT 1',
    'ALTER TABLE ingredients ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE AFTER unit'
);
PREPARE ingredient_active_stmt FROM @ingredient_active_ddl;
EXECUTE ingredient_active_stmt;
DEALLOCATE PREPARE ingredient_active_stmt;
*/
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: aura_cafe_erp
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `aura_cafe_erp`
--

/*!40000 DROP DATABASE IF EXISTS `aura_cafe_erp`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `aura_cafe_erp` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `aura_cafe_erp`;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'Cà phê máy','coffee_maker','https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=600&auto=format&fit=crop&q=80','ACTIVE'),(2,'Cà phê truyền thống','local_cafe','https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80','ACTIVE'),(3,'Trà trái cây','emoji_food_beverage','https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&auto=format&fit=crop&q=80','ACTIVE'),(4,'Đá xay & Matcha','icecream','https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&auto=format&fit=crop&q=80','ACTIVE'),(5,'Bánh ngọt','bakery_dining','https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&auto=format&fit=crop&q=80','ACTIVE'),(6,'Đóng chai','liquor','https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=600&auto=format&fit=crop&q=80','ACTIVE');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category_images`
--

DROP TABLE IF EXISTS `category_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_images` (
  `category_id` int NOT NULL,
  `image_data` mediumblob NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  CONSTRAINT `fk_category_image_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category_images`
--

LOCK TABLES `category_images` WRITE;
/*!40000 ALTER TABLE `category_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `category_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_reviews`
--

DROP TABLE IF EXISTS `customer_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_reviews` (
  `id` int NOT NULL AUTO_INCREMENT,
  `ticket_code` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_id` int NOT NULL,
  `customer_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_id` int DEFAULT NULL,
  `order_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issue_type` enum('RATING','COMPLAINT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `rating` int DEFAULT NULL,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `images` text COLLATE utf8mb4_unicode_ci,
  `contact_method` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'phone',
  `status` enum('PENDING','PROCESSING','RESOLVED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `status_label` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `admin_response` text COLLATE utf8mb4_unicode_ci,
  `progress_percent` int NOT NULL DEFAULT '25',
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_note` text COLLATE utf8mb4_unicode_ci,
  `resolution` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ticket_code` (`ticket_code`),
  KEY `idx_reviews_customer` (`customer_id`),
  KEY `idx_reviews_order` (`order_id`),
  CONSTRAINT `fk_review_customer` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_review_type` CHECK ((((`issue_type` = _utf8mb4'RATING') and (`rating` between 1 and 5)) or ((`issue_type` = _utf8mb4'COMPLAINT') and ((`rating` is null) or (`rating` between 1 and 5)))))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_reviews`
--

LOCK TABLES `customer_reviews` WRITE;
/*!40000 ALTER TABLE `customer_reviews` DISABLE KEYS */;
INSERT INTO `customer_reviews` VALUES (1,NULL,4,NULL,NULL,1,NULL,'RATING',5,NULL,NULL,NULL,'Good coffee and friendly service',NULL,'phone','RESOLVED',NULL,'Thank you for your feedback',25,NULL,NULL,NULL,'2026-10-04 09:30:00','2026-10-28 14:26:58'),(2,NULL,5,NULL,NULL,2,NULL,'COMPLAINT',NULL,NULL,NULL,NULL,'Delivery took longer than expected',NULL,'phone','PENDING',NULL,NULL,25,NULL,NULL,NULL,'2026-10-03 10:41:00','2026-10-28 14:26:58'),(3,NULL,5,NULL,NULL,3,NULL,'RATING',4,NULL,NULL,NULL,'Good taste, but a little too sweet',NULL,'phone','PENDING',NULL,NULL,25,NULL,NULL,NULL,'2026-10-03 11:20:00','2026-10-28 14:26:58'),(4,'#FB-2024-089',4,'Le Minh Anh','0900000004',NULL,'#AUR-86410','COMPLAINT',2,'wrong_size','Sai kích cỡ / Món','Giao sai size ly từ L xuống M','Ly Bạc Xỉu Kem Trứng của tôi bị nhầm thành size M thay vì size L như đã đặt trên hóa đơn.','https://lh3.googleusercontent.com/aida-public/AB6AXuDmpTys3KoN-gBpIZ2NHZpRJuTwcv-5IYk5GLa4_0uVM_hhieERDkGhxBxjxrEkep1JSAnjV_4SPCgWBVKnMb6fwivTCsPDIZrULPy7mL5ADYCvY6PIwelzBqc3MDBhcvjIWD8AIfWl4SwKLbsFrj0d1ysYfNX_B5EnxoRyDrgKDFgvdcyO-Ms8_F5cd-FImpqdFCuvDZb5yW0IJIY2nfBTxVlU5v0Ss4rTwrmL4ZtFkSiyLaAxWJKC','phone','PROCESSING','Đang xử lý','Đang kiểm tra phản ánh của khách hàng.',65,'Thu Hà','Đang kiểm tra phản ánh của khách hàng.',NULL,'2024-10-20 14:45:00','2026-10-28 14:26:58'),(5,'#FB-2024-042',4,'Le Minh Anh','0900000004',NULL,'#AUR-78219','COMPLAINT',3,'packaging','Thiếu ống hút/đồ ăn','Thiếu ống hút và khăn giấy','Đơn hàng giao thiếu ống hút và khăn giấy khi tôi nhận tại văn phòng.','','chat','RESOLVED','Đã giải quyết xong','Aura chân thành xin lỗi quý khách về sự bất tiện trên.',100,'Thanh Trúc','Cửa hàng đã kiểm điểm khâu kiểm tra trước khi bàn giao cho shipper.','Aura chân thành xin lỗi quý khách về sự bất tiện trên.','2024-10-16 10:15:00','2026-10-28 14:26:58');
/*!40000 ALTER TABLE `customer_reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `deliveries`
--

DROP TABLE IF EXISTS `deliveries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deliveries` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `shipper_id` int NOT NULL,
  `status` enum('ASSIGNED','ACCEPTED','DELIVERING','DELIVERED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ASSIGNED',
  `cod_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `cod_collected` decimal(12,2) DEFAULT NULL,
  `cod_handed_over_at` datetime DEFAULT NULL,
  `cod_receiver_id` int DEFAULT NULL,
  `assigned_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `accepted_at` datetime DEFAULT NULL,
  `picked_up_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `delivery_note` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_delivery_order` (`order_id`),
  KEY `fk_delivery_cod_receiver` (`cod_receiver_id`),
  KEY `idx_delivery_shipper_status` (`shipper_id`,`status`),
  CONSTRAINT `fk_delivery_cod_receiver` FOREIGN KEY (`cod_receiver_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_delivery_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_delivery_shipper` FOREIGN KEY (`shipper_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_delivery_cod` CHECK (((`cod_amount` >= 0) and ((`cod_collected` is null) or (`cod_collected` >= 0))))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `deliveries`
--

LOCK TABLES `deliveries` WRITE;
/*!40000 ALTER TABLE `deliveries` DISABLE KEYS */;
INSERT INTO `deliveries` VALUES (1,2,6,'DELIVERING',0.00,NULL,NULL,NULL,'2026-10-03 10:20:00','2026-10-03 10:22:00','2026-10-03 10:35:00',NULL,NULL);
/*!40000 ALTER TABLE `deliveries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `delivery_issues`
--

DROP TABLE IF EXISTS `delivery_issues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_issues` (
  `id` int NOT NULL AUTO_INCREMENT,
  `delivery_id` int NOT NULL,
  `issue_type` enum('CUSTOMER_UNREACHABLE','WRONG_ADDRESS','CUSTOMER_REFUSED','DAMAGED','TRAFFIC','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_issue_delivery` (`delivery_id`),
  CONSTRAINT `fk_issue_delivery` FOREIGN KEY (`delivery_id`) REFERENCES `deliveries` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `delivery_issues`
--

LOCK TABLES `delivery_issues` WRITE;
/*!40000 ALTER TABLE `delivery_issues` DISABLE KEYS */;
/*!40000 ALTER TABLE `delivery_issues` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ingredients`
--

DROP TABLE IF EXISTS `ingredients`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingredients` (
  `id` int NOT NULL AUTO_INCREMENT,
  `supplier_id` int DEFAULT NULL,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `current_stock` decimal(12,3) NOT NULL DEFAULT '0.000',
  `min_stock_level` decimal(12,3) NOT NULL DEFAULT '0.000',
  PRIMARY KEY (`id`),
  KEY `idx_ingredients_supplier` (`supplier_id`),
  CONSTRAINT `fk_ingredient_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_ingredient_stock` CHECK (((`current_stock` >= 0) and (`min_stock_level` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ingredients`
--

LOCK TABLES `ingredients` WRITE;
/*!40000 ALTER TABLE `ingredients` DISABLE KEYS */;
INSERT INTO `ingredients` (`id`,`supplier_id`,`name`,`unit`,`current_stock`,`min_stock_level`) VALUES (1,1,'Coffee beans','gram',10000.000,2000.000),(2,2,'Fresh milk','ml',20000.000,5000.000),(3,2,'Peach syrup','ml',5000.000,1000.000),(4,2,'Black tea','gram',8000.000,1500.000),(5,2,'Tapioca pearls','gram',4000.000,1000.000),(6,3,'Cheesecake slice','piece',30.000,5.000),(7,2,'Orange','piece',100.000,20.000),(8,2,'Lemon','piece',80.000,15.000);
/*!40000 ALTER TABLE `ingredients` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_transactions`
--

DROP TABLE IF EXISTS `inventory_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_transactions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `ingredient_id` int NOT NULL,
  `transaction_type` enum('IN_PURCHASE','OUT_ORDER','ADJUSTMENT','SPOILAGE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `reference_type` enum('ORDER','PURCHASE_ORDER','ADJUSTMENT') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ADJUSTMENT',
  `reference_id` int DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inventory_ingredient` (`ingredient_id`),
  KEY `idx_inventory_reference` (`reference_type`,`reference_id`),
  CONSTRAINT `fk_inventory_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredients` (`id`),
  CONSTRAINT `chk_inventory_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_transactions`
--

LOCK TABLES `inventory_transactions` WRITE;
/*!40000 ALTER TABLE `inventory_transactions` DISABLE KEYS */;
INSERT INTO `inventory_transactions` VALUES (1,1,'IN_PURCHASE',5000.000,'PURCHASE_ORDER',1,NULL,'2026-10-01 10:00:00'),(2,2,'IN_PURCHASE',10000.000,'PURCHASE_ORDER',2,NULL,'2026-10-02 10:00:00'),(3,1,'OUT_ORDER',36.000,'ORDER',1,NULL,'2026-10-03 09:10:00'),(4,2,'OUT_ORDER',360.000,'ORDER',2,NULL,'2026-10-03 10:10:00'),(5,5,'SPOILAGE',100.000,'ADJUSTMENT',NULL,NULL,'2026-10-03 11:00:00'),(6,3,'ADJUSTMENT',50.000,'ADJUSTMENT',NULL,NULL,'2026-10-03 11:30:00'),(7,2,'IN_PURCHASE',10000.000,'PURCHASE_ORDER',4,NULL,'2026-10-04 09:00:00'),(8,3,'IN_PURCHASE',5000.000,'PURCHASE_ORDER',4,NULL,'2026-10-04 09:00:00'),(9,4,'IN_PURCHASE',8000.000,'PURCHASE_ORDER',4,NULL,'2026-10-04 09:00:00'),(10,5,'IN_PURCHASE',4000.000,'PURCHASE_ORDER',4,NULL,'2026-10-04 09:00:00'),(11,7,'IN_PURCHASE',100.000,'PURCHASE_ORDER',4,NULL,'2026-10-04 09:00:00'),(12,8,'IN_PURCHASE',80.000,'PURCHASE_ORDER',4,NULL,'2026-10-04 09:00:00'),(13,6,'IN_PURCHASE',20.000,'PURCHASE_ORDER',5,NULL,'2026-10-04 09:30:00');
/*!40000 ALTER TABLE `inventory_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loyalty_transactions`
--

DROP TABLE IF EXISTS `loyalty_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loyalty_transactions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `order_id` int DEFAULT NULL,
  `points_change` int NOT NULL,
  `transaction_type` enum('EARN','REDEEM','REFUND','ADJUST') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_loyalty_order` (`order_id`),
  KEY `idx_loyalty_customer` (`customer_id`),
  CONSTRAINT `fk_loyalty_customer` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_loyalty_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_loyalty_points` CHECK ((((`transaction_type` = _utf8mb4'EARN') and (`points_change` > 0)) or ((`transaction_type` = _utf8mb4'REDEEM') and (`points_change` < 0)) or ((`transaction_type` = _utf8mb4'REFUND') and (`points_change` > 0)) or ((`transaction_type` = _utf8mb4'ADJUST') and (`points_change` <> 0))))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loyalty_transactions`
--

LOCK TABLES `loyalty_transactions` WRITE;
/*!40000 ALTER TABLE `loyalty_transactions` DISABLE KEYS */;
INSERT INTO `loyalty_transactions` VALUES (1,4,1,80,'EARN','2026-10-28 14:26:58'),(2,5,2,100,'EARN','2026-10-28 14:26:58');
/*!40000 ALTER TABLE `loyalty_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `product_id` int NOT NULL,
  `product_name_at_time` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `customization` json DEFAULT NULL,
  `quantity` int NOT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_order_item_product` (`product_id`),
  KEY `idx_order_items_order` (`order_id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `chk_order_item_price` CHECK (((`unit_price` >= 0) and (`subtotal` >= 0))),
  CONSTRAINT `chk_order_item_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (1,1,1,'Cà Phê Muối Aura Đặc Biệt',45000.00,'{\"size\": \"M\"}',2,90000.00),(2,2,5,'Trà Đào Cam Sả Tươi',45000.00,'{\"size\": \"M\"}',1,45000.00),(3,2,12,'Bánh Cheesecake Việt Quất',55000.00,NULL,1,55000.00),(4,9,1,'Cà Phê Muối Aura Đặc Biệt',45000.00,'{\"size\": \"M\", \"notes\": \"100% đường, 100% đá\", \"toppings\": []}',1,45000.00),(5,10,1,'Cà Phê Muối Aura Đặc Biệt',45000.00,'{\"size\": \"M\", \"notes\": \"100% đường, 100% đá\", \"toppings\": []}',1,45000.00);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_id` int DEFAULT NULL,
  `cashier_id` int DEFAULT NULL,
  `order_type` enum('DINE_IN','TAKEAWAY','ONLINE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PENDING','CONFIRMED','PREPARING','READY','DELIVERING','COMPLETED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `total_amount` decimal(12,2) NOT NULL,
  `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `delivery_fee` decimal(12,2) NOT NULL DEFAULT '0.00',
  `final_amount` decimal(12,2) NOT NULL,
  `recipient_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recipient_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `delivery_address` text COLLATE utf8mb4_unicode_ci,
  `shipper_id` int DEFAULT NULL,
  `table_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_note` text COLLATE utf8mb4_unicode_ci,
  `scheduled_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_code` (`order_code`),
  KEY `fk_order_cashier` (`cashier_id`),
  KEY `fk_order_shipper` (`shipper_id`),
  KEY `idx_orders_customer` (`customer_id`),
  KEY `idx_orders_status` (`status`),
  KEY `idx_orders_created_at` (`created_at`),
  CONSTRAINT `fk_order_cashier` FOREIGN KEY (`cashier_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_order_customer` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_order_shipper` FOREIGN KEY (`shipper_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_order_amounts` CHECK (((`total_amount` >= 0) and (`discount_amount` >= 0) and (`delivery_fee` >= 0) and (`final_amount` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,'AURA-001',4,3,'DINE_IN','COMPLETED',90000.00,9000.00,0.00,81000.00,NULL,NULL,NULL,NULL,'T01','Less ice',NULL,'2026-10-03 09:00:00'),(2,'AURA-002',5,3,'ONLINE','DELIVERING',100000.00,0.00,15000.00,115000.00,'Pham Quang Huy','0900000005','Cau Giay, Hanoi',6,NULL,'Call before delivery',NULL,'2026-10-03 10:00:00'),(3,'ORD-20261003-01',5,3,'DINE_IN','COMPLETED',96000.00,0.00,0.00,96000.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-03 12:30:00'),(4,'ORD-20261003-02',5,3,'TAKEAWAY','COMPLETED',120000.00,0.00,0.00,120000.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-03 13:45:00'),(5,'ORD-20261003-03',5,3,'DINE_IN','COMPLETED',80000.00,0.00,0.00,80000.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-03 15:15:00'),(6,'ORD-20261003-04',5,3,'DINE_IN','COMPLETED',110000.00,0.00,0.00,110000.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-03 14:10:00'),(7,'ORD-20261003-05',5,3,'TAKEAWAY','COMPLETED',105000.00,0.00,0.00,105000.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-03 14:50:00'),(8,'ORD-20261003-06',5,3,'DINE_IN','COMPLETED',100000.00,0.00,0.00,100000.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-03 16:20:00'),(9,'ORD-261028-3516',NULL,3,'DINE_IN','CANCELLED',45000.00,0.00,0.00,45000.00,'Khách tại quán','',NULL,NULL,NULL,'',NULL,'2026-10-28 16:22:13'),(10,'ORD-261028-3021',NULL,3,'DINE_IN','CONFIRMED',45000.00,0.00,0.00,45000.00,'Khách tại quán','',NULL,NULL,NULL,'',NULL,'2026-10-28 16:40:23');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `payment_method` enum('CASH','VNPAY','MOMO','BANK_TRANSFER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `status` enum('PENDING','SUCCESS','FAILED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `paid_at` datetime DEFAULT NULL,
  `payment_note` text COLLATE utf8mb4_unicode_ci,
  `gateway_reference` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `callback_payload` json DEFAULT NULL,
  `callback_received_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_payment_transaction_code` (`transaction_code`),
  KEY `idx_payments_order` (`order_id`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_payment_amount` CHECK ((`amount` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (1,1,'CASH',NULL,81000.00,'SUCCESS','2026-10-03 09:05:00','Paid at counter',NULL,NULL,NULL),(2,2,'MOMO','MOMO-DEMO-002',115000.00,'SUCCESS','2026-10-03 10:05:00','Online payment',NULL,NULL,NULL),(3,3,'CASH','CSH-1003-01',96000.00,'SUCCESS','2026-10-03 12:32:00',NULL,NULL,NULL,NULL),(4,4,'CASH','CSH-1003-02',120000.00,'SUCCESS','2026-10-03 13:47:00',NULL,NULL,NULL,NULL),(5,5,'CASH','CSH-1003-03',80000.00,'SUCCESS','2026-10-03 15:16:00',NULL,NULL,NULL,NULL),(6,6,'VNPAY','VNP-1003-01',110000.00,'SUCCESS','2026-10-03 14:12:00',NULL,NULL,NULL,NULL),(7,7,'MOMO','MOM-1003-01',105000.00,'SUCCESS','2026-10-03 14:51:00',NULL,NULL,NULL,NULL),(8,8,'MOMO','MOM-1003-02',100000.00,'SUCCESS','2026-10-03 16:22:00',NULL,NULL,NULL,NULL),(9,9,'CASH','POS-ORD-261028-3516',45000.00,'FAILED','2026-10-28 16:22:13','Thanh toán tại quầy qua Tiền mặt [Đã hủy đơn bởi Thu ngân]',NULL,NULL,NULL),(10,10,'CASH','POS-ORD-261028-3021',45000.00,'SUCCESS','2026-10-28 16:40:23','Thanh toán tại quầy qua Tiền mặt',NULL,NULL,NULL);
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_images` (
  `product_id` int NOT NULL,
  `image_data` mediumblob NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  CONSTRAINT `fk_product_image_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_images`
--

LOCK TABLES `product_images` WRITE;
/*!40000 ALTER TABLE `product_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_ingredients`
--

DROP TABLE IF EXISTS `product_ingredients`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_ingredients` (
  `id` int NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `ingredient_id` int NOT NULL,
  `quantity_required` decimal(12,3) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_product_ingredient` (`product_id`,`ingredient_id`),
  KEY `fk_bom_ingredient` (`ingredient_id`),
  CONSTRAINT `fk_bom_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredients` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bom_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_quantity_required` CHECK ((`quantity_required` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_ingredients`
--

LOCK TABLES `product_ingredients` WRITE;
/*!40000 ALTER TABLE `product_ingredients` DISABLE KEYS */;
INSERT INTO `product_ingredients` VALUES (2,2,1,18.000),(3,2,2,180.000),(4,3,1,30.000),(5,4,1,18.000),(6,4,2,100.000),(7,5,3,30.000),(8,5,4,10.000),(9,5,7,1.000),(10,8,4,10.000),(11,9,1,18.000),(12,10,1,18.000),(13,10,2,150.000),(14,12,6,1.000),(18,1,1,18.000);
/*!40000 ALTER TABLE `product_ingredients` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` int NOT NULL AUTO_INCREMENT,
  `category_id` int NOT NULL,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `base_price` decimal(12,2) NOT NULL,
  `variants_config` json DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_available` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_products_category` (`category_id`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `chk_product_price` CHECK ((`base_price` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,2,'Cà Phê Muối Aura Đặc Biệt',45000.00,'{\"sizes\": {\"L\": 10000, \"M\": 0}, \"toppings\": [{\"name\": \"Extra kem muối\", \"price\": 8000}, {\"name\": \"Extra shot espresso\", \"price\": 10000}]}','Fine Robusta ủ phin truyền thống phối kem sữa mằn mặn béo ngậy, cân bằng hậu vị ngọt đầm.','https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80',1),(2,1,'Caramel Macchiato Đặc Sản',55000.00,'{\"sizes\": {\"L\": 10000, \"M\": 0}, \"toppings\": [{\"name\": \"Sốt caramel thêm\", \"price\": 5000}, {\"name\": \"Kem whipping\", \"price\": 8000}]}','Lớp sữa tươi nóng thanh dịu, quyện cùng espresso kép Arabica Cầu Đất đậm đà và vân sốt caramel thơm lừng.','https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=600&auto=format&fit=crop&q=80',1),(3,6,'Cold Brew Đóng Chai Aura Reserve',68000.00,'{\"sizes\": {\"Chai 250ml\": 0, \"Chai 500ml\": 35000}, \"toppings\": []}','Chiết xuất lạnh 16 tiếng từ 100% Arabica Bourbon, hương hoa quả nhiệt đới tươi mát, giữ trọn vị nguyên bản.','https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=600&auto=format&fit=crop&q=80',1),(4,2,'Cà Phê Sữa Đá Sài Gòn Nâu',39000.00,'{\"sizes\": {\"L\": 8000, \"M\": 0}, \"toppings\": [{\"name\": \"Thạch cà phê\", \"price\": 6000}, {\"name\": \"Extra shot phin\", \"price\": 8000}]}','Hạt Fine Robusta rang nâu bóng mượt, chiết xuất phin chậm kết hợp sữa đặc ngọt êm béo ngậy truyền thống.','https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=600&auto=format&fit=crop&q=80',1),(5,3,'Trà Đào Cam Sả Tươi',45000.00,'{\"sizes\": {\"L\": 10000, \"M\": 0}, \"toppings\": [{\"name\": \"Thạch đào giòn\", \"price\": 8000}, {\"name\": \"Hạt chia\", \"price\": 5000}]}','Nước cốt trà đen Ceylon thanh dịu kết hợp miếng đào giòn ngọt mọng nước cùng sả tươi ấm nồng.','https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&auto=format&fit=crop&q=80',1),(6,5,'Bánh Croissant Bơ Pháp',35000.00,'{\"sizes\": {\"Tiêu chuẩn\": 0}, \"toppings\": [{\"name\": \"Mứt dâu tây\", \"price\": 6000}, {\"name\": \"Sốt sôcôla\", \"price\": 6000}]}','Bột ngàn lớp thủ công giòn rụm bên ngoài, mềm xốp ngậy hương bơ Elle & Vire hảo hạng bên trong.','https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&auto=format&fit=crop&q=80',1),(7,4,'Matcha Latte Uji Kyoto Đá Xay',58000.00,'{\"sizes\": {\"L\": 12000, \"M\": 0}, \"toppings\": [{\"name\": \"Kem whipping\", \"price\": 8000}, {\"name\": \"Thạch matcha\", \"price\": 8000}]}','Bột trà xanh Uji trứ danh cố đô Kyoto xay cùng sữa hạt và kem whipping bông xốp béo mịn thượng hạng.','https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&auto=format&fit=crop&q=80',1),(8,3,'Trà Sen Vàng Long Nhãn',49000.00,'{\"sizes\": {\"L\": 10000, \"M\": 0}, \"toppings\": [{\"name\": \"Hạt sen thêm\", \"price\": 10000}, {\"name\": \"Củ năng giòn\", \"price\": 8000}]}','Hạt sen Huế ninh mềm dẻo, nhãn nhục ngọt thanh quyện cùng trà ô long hảo hạng và lớp kem củ năng giòn.','https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop&q=80',1),(9,1,'Americano Đá Rang Đậm',35000.00,'{\"sizes\": {\"L\": 10000, \"M\": 0}, \"toppings\": [{\"name\": \"Extra shot espresso\", \"price\": 10000}]}','Espresso kép chiết xuất áp suất cao từ hạt cà phê đặc sản, vị chua thanh thanh tao và hậu ngọt dài lâu.','https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&auto=format&fit=crop&q=80',1),(10,1,'Latte Nghệ Thuật Hạt Phỉ',52000.00,'{\"sizes\": {\"L\": 10000, \"M\": 0}, \"toppings\": [{\"name\": \"Siro hạt phỉ\", \"price\": 6000}, {\"name\": \"Sữa yến mạch (Oat milk)\", \"price\": 12000}]}','Hòa quyện tinh tế giữa espresso thơm ngát, sữa tươi đánh bọt mịn màng và siro hạt phỉ thơm lừng.','https://images.unsplash.com/photo-1534778101976-62847782c213?w=600&auto=format&fit=crop&q=80',1),(11,4,'Sôcôla Bỉ Đá Xay Hạnh Nhân',55000.00,'{\"sizes\": {\"L\": 10000, \"M\": 0}, \"toppings\": [{\"name\": \"Kem whipping\", \"price\": 8000}, {\"name\": \"Hạnh nhân vụn\", \"price\": 6000}]}','Bột cacao nguyên chất nhập khẩu Bỉ xay nhuyễn cùng sữa đặc, rắc hạnh nhân nướng giòn tan.','https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=600&auto=format&fit=crop&q=80',1),(12,5,'Bánh Cheesecake Việt Quất',55000.00,'{\"sizes\": {\"1 Phần\": 0}, \"toppings\": []}','Cream cheese Philadelphia béo ngậy kết hợp mứt việt quất chua ngọt dịu và đế bánh quy nướng thơm lừng.','https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=600&auto=format&fit=crop&q=80',1);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order_items`
--

DROP TABLE IF EXISTS `purchase_order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_items` (
  `id` int NOT NULL AUTO_INCREMENT,
  `po_id` int NOT NULL,
  `ingredient_id` int NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_poi_ingredient` (`ingredient_id`),
  KEY `idx_poi_order` (`po_id`),
  CONSTRAINT `fk_poi_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredients` (`id`),
  CONSTRAINT `fk_poi_order` FOREIGN KEY (`po_id`) REFERENCES `purchase_orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_purchase_price` CHECK ((`unit_price` >= 0)),
  CONSTRAINT `chk_purchase_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order_items`
--

LOCK TABLES `purchase_order_items` WRITE;
/*!40000 ALTER TABLE `purchase_order_items` DISABLE KEYS */;
INSERT INTO `purchase_order_items` VALUES (1,1,1,5000.000,400.00),(2,2,2,10000.000,150.00),(3,2,3,5000.000,120.00),(4,3,6,20.000,40000.00),(5,4,2,10000.000,150.00),(6,4,3,5000.000,120.00),(7,4,4,8000.000,120.00),(8,4,5,4000.000,100.00),(9,4,7,100.000,5000.00),(10,4,8,80.000,3000.00),(11,5,6,20.000,40000.00);
/*!40000 ALTER TABLE `purchase_order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_orders`
--

DROP TABLE IF EXISTS `purchase_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `supplier_id` int NOT NULL,
  `creator_id` int NOT NULL,
  `restock_request_id` int DEFAULT NULL,
  `total_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` enum('DRAFT','ORDERED','RECEIVED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ordered_at` datetime DEFAULT NULL,
  `received_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_po_restock_request` (`restock_request_id`),
  KEY `fk_po_creator` (`creator_id`),
  KEY `idx_po_supplier` (`supplier_id`),
  CONSTRAINT `fk_po_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_po_restock_request` FOREIGN KEY (`restock_request_id`) REFERENCES `restock_requests` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_po_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`),
  CONSTRAINT `chk_po_total` CHECK ((`total_cost` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_orders`
--

LOCK TABLES `purchase_orders` WRITE;
/*!40000 ALTER TABLE `purchase_orders` DISABLE KEYS */;
INSERT INTO `purchase_orders` VALUES (1,1,1,NULL,2000000.00,'RECEIVED',NULL,'2026-10-01 08:00:00',NULL,NULL),(2,2,1,NULL,1500000.00,'ORDERED',NULL,'2026-10-02 09:00:00',NULL,NULL),(3,3,1,NULL,800000.00,'DRAFT',NULL,'2026-10-03 08:00:00',NULL,NULL),(4,2,1,NULL,4200000.00,'RECEIVED',NULL,'2026-10-04 08:00:00',NULL,NULL),(5,3,1,NULL,800000.00,'RECEIVED',NULL,'2026-10-04 08:30:00',NULL,NULL);
/*!40000 ALTER TABLE `purchase_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `restock_request_items`
--

DROP TABLE IF EXISTS `restock_request_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `restock_request_items` (
  `id` int NOT NULL AUTO_INCREMENT,
  `request_id` int NOT NULL,
  `ingredient_id` int NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `item_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_restock_request_ingredient` (`request_id`,`ingredient_id`),
  KEY `fk_restock_item_ingredient` (`ingredient_id`),
  CONSTRAINT `fk_restock_item_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredients` (`id`),
  CONSTRAINT `fk_restock_item_request` FOREIGN KEY (`request_id`) REFERENCES `restock_requests` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_restock_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `restock_request_items`
--

LOCK TABLES `restock_request_items` WRITE;
/*!40000 ALTER TABLE `restock_request_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `restock_request_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `restock_requests`
--

DROP TABLE IF EXISTS `restock_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `restock_requests` (
  `id` int NOT NULL AUTO_INCREMENT,
  `requester_id` int NOT NULL,
  `reviewer_id` int DEFAULT NULL,
  `status` enum('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_restock_requester` (`requester_id`),
  KEY `fk_restock_reviewer` (`reviewer_id`),
  KEY `idx_restock_status` (`status`),
  CONSTRAINT `fk_restock_requester` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_restock_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `restock_requests`
--

LOCK TABLES `restock_requests` WRITE;
/*!40000 ALTER TABLE `restock_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `restock_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shift_handovers`
--

DROP TABLE IF EXISTS `shift_handovers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shift_handovers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `schedule_id` int NOT NULL,
  `cashier_id` int NOT NULL,
  `opening_cash` decimal(12,2) NOT NULL DEFAULT '0.00',
  `cash_sales` decimal(12,2) NOT NULL DEFAULT '0.00',
  `expected_cash` decimal(12,2) NOT NULL,
  `actual_cash` decimal(12,2) NOT NULL,
  `difference` decimal(12,2) NOT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  `status` enum('CONFIRMED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CONFIRMED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_handover_schedule` (`schedule_id`),
  KEY `fk_handover_cashier` (`cashier_id`),
  KEY `idx_handover_status` (`status`),
  CONSTRAINT `fk_handover_cashier` FOREIGN KEY (`cashier_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_handover_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `work_schedules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_handover_cash` CHECK (((`opening_cash` >= 0) and (`cash_sales` >= 0) and (`expected_cash` >= 0) and (`actual_cash` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shift_handovers`
--

LOCK TABLES `shift_handovers` WRITE;
/*!40000 ALTER TABLE `shift_handovers` DISABLE KEYS */;
INSERT INTO `shift_handovers` VALUES (1,12,3,0.00,45000.00,45000.00,45000.00,0.00,'ok','CONFIRMED','2026-10-28 16:41:30');
/*!40000 ALTER TABLE `shift_handovers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shop_settings`
--

DROP TABLE IF EXISTS `shop_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop_settings` (
  `setting_key` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `setting_value` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shop_settings`
--

LOCK TABLES `shop_settings` WRITE;
/*!40000 ALTER TABLE `shop_settings` DISABLE KEYS */;
INSERT INTO `shop_settings` VALUES ('delivery_fee','25000','2026-10-28 14:26:58'),('delivery_minutes','30','2026-10-28 14:26:58'),('freeship_threshold','150000','2026-10-28 14:26:58'),('payment_account_name','NGUYEN DUC QUANG MINH','2026-10-28 14:26:58'),('payment_account_no','234556688','2026-10-28 14:26:58'),('payment_bank_id','MBBank','2026-10-28 14:26:58'),('payment_bank_name','MBBank','2026-10-28 14:26:58'),('payment_callback_secret','AURA-LOCAL-DEMO-SECRET','2026-10-28 14:26:58'),('payment_gateway_url','https://payment-gateway.zemorts.workers.dev','2026-10-28 14:26:58'),('payment_test_mode','true','2026-10-28 14:26:58'),('points_earn_unit','10000','2026-10-28 14:26:58'),('points_step','10','2026-10-28 14:26:58'),('points_step_value','2000','2026-10-28 14:26:58');
/*!40000 ALTER TABLE `shop_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `suppliers`
--

DROP TABLE IF EXISTS `suppliers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `suppliers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` text COLLATE utf8mb4_unicode_ci,
  `status` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `suppliers`
--

LOCK TABLES `suppliers` WRITE;
/*!40000 ALTER TABLE `suppliers` DISABLE KEYS */;
INSERT INTO `suppliers` VALUES (1,'Fresh Bean Supplier','0911000001','Dong Da, Hanoi','ACTIVE'),(2,'Dairy and Syrup Co.','0911000002','Cau Giay, Hanoi','ACTIVE'),(3,'Bakery Ingredient Supply','0911000003','Ba Dinh, Hanoi','ACTIVE');
/*!40000 ALTER TABLE `suppliers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `birthday` date DEFAULT NULL,
  `address` text COLLATE utf8mb4_unicode_ci,
  `role` enum('ADMIN','STAFF','CUSTOMER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CUSTOMER',
  `status` enum('ACTIVE','INACTIVE','LOCKED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `position` enum('MANAGER','BARISTA','CASHIER','SHIPPER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `base_salary` decimal(12,2) NOT NULL DEFAULT '0.00',
  `hire_date` date DEFAULT NULL,
  `total_points` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone` (`phone`),
  CONSTRAINT `chk_user_points` CHECK ((`total_points` >= 0)),
  CONSTRAINT `chk_user_salary` CHECK ((`base_salary` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Aura Admin','0900000001','admin@auracafe.vn','demo_hash',NULL,NULL,'ADMIN','ACTIVE','MANAGER',15000000.00,'2025-01-01',0,'2026-10-28 14:26:58'),(2,'Nguyen Van An','0900000002','an@auracafe.vn','demo_hash',NULL,NULL,'STAFF','ACTIVE','BARISTA',8000000.00,'2025-02-01',0,'2026-10-28 14:26:58'),(3,'Tran Thi Binh','0900000003','binh@auracafe.vn','demo_hash',NULL,NULL,'STAFF','ACTIVE','CASHIER',8500000.00,'2025-02-01',0,'2026-10-28 14:26:58'),(4,'Le Minh Anh','0900000004','anh@gmail.com','demo_hash',NULL,NULL,'CUSTOMER','ACTIVE',NULL,0.00,NULL,120,'2026-10-28 14:26:58'),(5,'Pham Quang Huy','0900000005','huy@gmail.com','demo_hash',NULL,NULL,'CUSTOMER','ACTIVE',NULL,0.00,NULL,50,'2026-10-28 14:26:58'),(6,'Hoang Van Nam','0900000006','nam@auracafe.vn','demo_hash',NULL,NULL,'STAFF','ACTIVE','SHIPPER',7500000.00,'2025-03-01',0,'2026-10-28 14:26:58');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_schedules`
--

DROP TABLE IF EXISTS `work_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_schedules` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `work_date` date NOT NULL,
  `shift_id` int NOT NULL,
  `check_in_time` datetime DEFAULT NULL,
  `check_out_time` datetime DEFAULT NULL,
  `status` enum('SCHEDULED','PRESENT','ABSENT','COMPLETED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCHEDULED',
  `note` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_work_shift` (`user_id`,`work_date`,`shift_id`),
  KEY `fk_schedule_shift` (`shift_id`),
  CONSTRAINT `fk_schedule_shift` FOREIGN KEY (`shift_id`) REFERENCES `work_shifts` (`id`),
  CONSTRAINT `fk_schedule_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_schedules`
--

LOCK TABLES `work_schedules` WRITE;
/*!40000 ALTER TABLE `work_schedules` DISABLE KEYS */;
INSERT INTO `work_schedules` VALUES (1,2,'2026-10-03',1,'2026-10-03 08:00:00','2026-10-03 12:00:00','COMPLETED','Ca sáng pha chế'),(2,3,'2026-10-03',2,'2026-10-03 12:00:00',NULL,'PRESENT','Ca chiều thu ngân'),(3,2,'2026-10-04',2,'2026-10-04 12:05:00',NULL,'PRESENT','Ca chiều pha chế'),(4,6,'2026-10-03',3,NULL,NULL,'SCHEDULED','Ca tối giao hàng'),(5,3,'2026-10-04',3,NULL,NULL,'SCHEDULED','Ca tối thu ngân'),(7,3,'2026-10-29',7,NULL,NULL,'SCHEDULED',''),(9,2,'2026-10-28',8,NULL,NULL,'SCHEDULED',''),(10,3,'2026-10-30',8,NULL,NULL,'SCHEDULED',''),(11,3,'2026-10-28',9,NULL,NULL,'SCHEDULED',''),(12,3,'2026-10-28',8,NULL,'2026-10-28 16:41:30','COMPLETED','ok');
/*!40000 ALTER TABLE `work_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_shifts`
--

DROP TABLE IF EXISTS `work_shifts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_shifts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `week_start` date NOT NULL,
  `shift_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_shift_week_name` (`week_start`,`shift_name`),
  KEY `idx_shift_week` (`week_start`),
  CONSTRAINT `chk_shift_time` CHECK ((`end_time` > `start_time`))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_shifts`
--

LOCK TABLES `work_shifts` WRITE;
/*!40000 ALTER TABLE `work_shifts` DISABLE KEYS */;
INSERT INTO `work_shifts` VALUES (1,'2026-09-28','Ca sáng','07:00:00','12:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(2,'2026-09-28','Ca chiều','12:00:00','17:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(3,'2026-09-28','Ca tối','17:00:00','22:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(4,'2026-10-19','Ca sáng','07:00:00','12:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(5,'2026-10-19','Ca chiều','12:00:00','17:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(6,'2026-10-19','Ca tối','17:00:00','22:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(7,'2026-10-26','Ca sáng','07:00:00','12:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(8,'2026-10-26','Ca chiều','12:30:00','17:30:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(9,'2026-10-26','Ca tối','17:30:00','22:00:00','2026-10-28 14:26:58','2026-10-28 14:26:58'),(10,'2026-10-26','Ca đêm','23:03:00','23:30:00','2026-10-28 16:32:37','2026-10-28 16:32:37');
/*!40000 ALTER TABLE `work_shifts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'aura_cafe_erp'
--

--
-- Dumping routines for database 'aura_cafe_erp'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-28 23:12:14
