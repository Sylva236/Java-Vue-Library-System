# Simple Library Management System (Java & Vue)

> A full-stack library management system developed for the Zhejiang University Database Systems course. This application provides a complete set of features for library administration, including book management, borrowing, and user card services.


## ✨ 主要功能 (Features)

* **📚 图书管理 (Book Management):**
    * 支持图书的增、删、改、查。
    * 提供多条件动态查询（书名、作者、出版社等）与排序功能。
    * 支持从文件批量导入图书信息。

* **🔄 借阅管理 (Borrowing System):**
    * 实现完整的图书借阅和归还流程。
    * 通过事务和行锁 (`SELECT ... FOR UPDATE`) 保证高并发场景下库存数据的准确性，防止超借。
    * 记录和查询用户的借阅历史。

* **💳 借书证管理 (Card Management):**
    * 支持借书证的注册、信息修改与删除。
    * 删除用户时，会校验其是否有未归还的图书。

## 🛠️ 技术栈 (Tech Stack)

* **后端 (Backend):**
    * **Java**: 核心业务逻辑实现语言。
    * **Maven**: 项目与依赖管理工具。
    * **Java `HttpServer`**: 提供基础的RESTful API服务。
* **前端 (Frontend):**
    * **Vue.js (Vue 3)**: 用于构建用户界面的前端框架。
    * **Element Plus**: 基于Vue 3的UI组件库，提供丰富的界面元素。
    * **Node.js**: 作为前端开发环境。
* **数据库 (Database):**
    * **PostgreSQL / MySQL**: 关系型数据库，用于存储所有业务数据。

## 🚀 如何运行 (Getting Started)

### **环境要求**
* JDK 17+ (e.g., `corretto-17.0.16`)
* Maven 3.x
* Node.js 16+
* MySQL 8.0+ (或 PostgreSQL)

### **安装与启动**

1.  **克隆仓库**
    
    ```bash
    git clone [你仓库的URL]
    cd [你仓库的文件夹名]
    ```
    
2.  **启动后端**
    * 配置数据库：在MySQL或PostgreSQL中创建一个名为 `library` 的数据库。
    * (如果需要) 修改 `DatabaseConnector.java` 中的数据库连接信息。
    * 使用 IntelliJ IDEA打开项目，它会自动使用 Maven 加载依赖。
    * 运行程序主入口。

3.  **启动前端**
    
    ```bash
    # 进入前端代码目录 (假设你的前端代码在 'frontend' 文件夹下)
    cd frontend 
    
    # 安装依赖
    npm install
    
    # 启动开发服务器
    npm run serve
    ```
4.  打开浏览器访问 `http://localhost:8080` (或其他你配置的前端端口)。

## 📜 许可证 (License)

This project is licensed under the MIT License.
