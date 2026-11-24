-- Bookstore database schema (Iteration 2)
-- 与前端 Data.json 字段对齐，并新增购物车、订单相关表

DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `cart_item`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `book`;

CREATE TABLE `book` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `author` varchar(100) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `isbn` varchar(50) DEFAULT NULL,
  `cover` varchar(500) DEFAULT NULL,
  `description` text,
  `publisher` varchar(100) DEFAULT NULL,
  `publish_date` varchar(20) DEFAULT NULL,
  `pages` int DEFAULT NULL,
  `stock` int NOT NULL DEFAULT 100,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL UNIQUE,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'customer',
  `disabled` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cart_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `book_id` int NOT NULL,
  `quantity` int NOT NULL DEFAULT 1,
  `added_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  CONSTRAINT `fk_cart_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `total_price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT '已支付',
  `order_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `book_id` int NOT NULL,
  `title` varchar(200) NOT NULL,
  `author` varchar(100) DEFAULT NULL,
  `cover` varchar(500) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `quantity` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  CONSTRAINT `fk_orderitem_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `book` (`title`, `author`, `price`, `isbn`, `cover`, `description`, `publisher`, `publish_date`, `pages`, `stock`) VALUES
('三体', '刘慈欣', 49.90, '978-7-5366-9293-0', 'https://img.doooor.com/img/forum/202306/20/152915m79us70mmkummtm8.jpg',
 '《三体》是刘慈欣创作的系列长篇科幻小说，由《三体》《三体Ⅱ·黑暗森林》《三体Ⅲ·死神永生》组成，第一部于2006年5月起在《科幻世界》杂志上连载，讲述了地球人类文明和三体文明的信息交流、生死搏杀及两个文明在宇宙中的兴衰历程。',
 '重庆出版社', '2008-01', 302, 100),
('城南旧事', '林海音', 29.90, '978-7-02-000000-0', 'https://ts2.tc.mm.bing.net/th/id/OIP-C.tPoXyq_DNPsE3xXg7vpjogHaHa?rs=1&pid=ImgDetMain&o=7&rm=3',
 '《城南旧事》是台湾女作家林海音的代表作。该作品通过英子童稚的双眼对童年往事的回忆，讲述了一段关于英子童年时的故事，反映了作者对童年的怀念和对北京城南的思念。',
 '人民文学出版社', '1960-01', 120, 100),
('未知的探索', '未知', 35.00, '978-7-000-00000-1', 'https://ts4.tc.mm.bing.net/th/id/OIP-C.7VRPVN8q77Yey_j0IUL3nAHaLH?rs=1&pid=ImgDetMain&o=7&rm=3',
 '这是一本关于科学探索的书籍，带领读者进入未知的科学世界，探索宇宙的奥秘和人类未知的领域。',
 '科学出版社', '2020-01', 280, 100),
('故事新编', '鲁迅', 59.00, '978-7-000-00000-2', 'https://ts2.tc.mm.bing.net/th/id/OIP-C.yvz65pOLos0Cs0Sik2xIOAHaHa?rs=1&pid=ImgDetMain&o=7&rm=3',
 '《故事新编》是鲁迅的一部短篇小说集，收录了《补天》《奔月》《理水》《采薇》《铸剑》《出关》《非攻》《起死》八篇小说。',
 '人民文学出版社', '1936-01', 150, 100),
('Java核心技术', 'Cay S. Horstmann', 45.00, '978-7-000-00000-3', 'https://ts4.tc.mm.bing.net/th/id/OIP-C.cb7vG9W0HfwFJH96N1gO8AHaHa?rs=1&pid=ImgDetMain&o=7&rm=3',
 '《Java核心技术》是2008年由机械工业出版社出版的图书，作者是Cay S. Horstmann。本书是Java领域最有影响力和价值的著作之一。',
 '机械工业出版社', '2008-01', 800, 100),
('红楼梦', '曹雪芹', 69.90, '978-7-000-00000-4', 'https://ts4.tc.mm.bing.net/th/id/OIP-C.i4E79ILzhTkIB8jXBBfMYQHaLH?rs=1&pid=ImgDetMain&o=7&rm=3',
 '《红楼梦》是中国古代章回体长篇小说，中国古典四大名著之一。小说以贾、史、王、薛四大家族的兴衰为背景，以富贵公子贾宝玉为视角，以贾宝玉与林黛玉、薛宝钗的爱情婚姻悲剧为主线。',
 '人民文学出版社', '1791-01', 1200, 100),
('微服务架构设计模式', 'Chris Richardson', 99.00, '978-7-111-62530-8', 'https://via.placeholder.com/300x400?text=Microservices',
 '本书由微服务架构专家 Chris Richardson 撰写，系统讲解了微服务架构的设计模式：服务拆分、API 网关、进程间通信、分布式事务（Saga）、事件驱动、服务发现、可观测性与部署。适合希望把单体应用迁移到微服务架构的 Java 工程师与架构师阅读。',
 '机械工业出版社', '2019-04', 400, 60),
('流浪地球', '刘慈欣', 32.00, '978-7-5354-8000-1', 'https://via.placeholder.com/300x400?text=Wandering+Earth',
 '《流浪地球》是刘慈欣的科幻小说代表作之一，讲述太阳即将毁灭，人类建造行星发动机带着地球逃离太阳系的故事。',
 '长江文艺出版社', '2017-01', 250, 80);

INSERT INTO `user` (`username`, `password`, `email`, `avatar`, `role`, `disabled`) VALUES
('toothache', '123456', 'toothache@example.com', 'https://via.placeholder.com/40', 'customer', 0);
