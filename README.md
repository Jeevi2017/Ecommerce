🛍️ E-Commerce Proof of Concept

(Angular • Spring Boot)

This repository contains a full-stack Proof of Concept (POC) for an enterprise-ready e-commerce system. The application demonstrates a modular, scalable architecture using Angular on the frontend and Spring Boot on the backend, integrated with MySQL, Apache Kafka, and ElasticSearch.

✨ Core Capabilities

Product Management & Wishlist Handling

End-to-End Cart and Order Processing

REST-based Backend Services with High Scalability

Event-Driven Communication via Kafka

Optimized Product Search using ElasticSearch

⚙️ Local Setup Instructions

Follow the steps below to run the application on your local machine.

1️⃣ Clone the Repository
git clone https://github.com/Jeevi2017/Ecommerce.git

2️⃣ Database Configuration

Start MySQL and create the required database:

CREATE DATABASE ecomdb;

🧰 System Requirements

Ensure the following dependencies are installed before running the project:

Node.js & npm (LTS version 18 or higher)

Angular CLI

npm install -g @angular/cli


Java JDK — version 17 or above

Apache Maven — for backend build and execution

Supporting Services (must be running):

MySQL

Apache Kafka

ElasticSearch

🗂️ Database Structure

The application uses the following core tables:

User

Address

Profile

Customer_Order

Cart

Cart_Items

Discounts

Order_Item

Payments

Product

Product_Categories

Product_Images

Product_sizes

Reviews

Wishlist
