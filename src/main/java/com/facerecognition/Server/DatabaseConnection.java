package com.facerecognition.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private final String URL = "jdbc:postgresql://localhost:5432/recognition";
    private final String USER = "postgres";
    private final String PASSWORD = "654321";

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(this.URL, this.USER, this.PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean testDatabaseConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(this.URL, this.USER, this.PASSWORD);
            if (connection != null) {
                System.out.println("Connected to the database!");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}

