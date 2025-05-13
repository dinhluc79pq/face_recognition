package com.facerecognition.Server;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserController {
    private List<User> userList;

    public UserController(){
        userList = new ArrayList<>();
    }
    
    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public boolean checkExistUser(String uid) {
        for (User user : this.userList) {
            if (user.getUid().equals(uid)) {
                return true;
            }
        }
        return false;
    }

    public void getListUserDatabase() {
        List<User> Users = new ArrayList<>();
        String sql = "SELECT uid, name, dob, avata, face_encoding FROM userrecognition";
        Connection connection = new DatabaseConnection().getConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String uid = rs.getString("uid");
                String name = rs.getString("name");
                LocalDate dob = null;
                if (rs.getDate("dob") != null) {
                    dob = rs.getDate("dob").toLocalDate();
                }
                String avata = rs.getString("avata");
                List<Double> faceEncoding = null;
                if (rs.getArray("face_encoding") != null) {
                    Array array = rs.getArray("face_encoding");
                    Double[] doubleArray = (Double[]) array.getArray();
                    faceEncoding = Arrays.asList(doubleArray);
                }
            
                User User = new User(uid, name, dob, avata, faceEncoding);
                Users.add(User);
            }
            this.userList = Users;
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
    }

    public boolean insertUser(User user) {
        if (checkExistUser(user.getUid())) {
            System.out.println("Đã tồn tại user!");
            return false;
        }
        String sql = "INSERT INTO userrecognition (uid, name, dob, avata, face_encoding) VALUES (?, ?, ?, ?, ?)";
        Connection connection = new DatabaseConnection().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Thiết lập các giá trị cho PreparedStatement
            statement.setString(1, user.getUid());
            statement.setString(2, user.getName());

            if (user.getDob() != null) {
               statement.setDate(3, java.sql.Date.valueOf(user.getDob())); 
            }
            else {
                statement.setDate(3, null); 
            }
            
            statement.setString(4, user.getAvata());
            List<Double> encodingList = user.getFaceEncoding();
            if (encodingList != null) {
                Double[] encodingArray = encodingList.toArray(new Double[0]);
                Array faceEncoding = connection.createArrayOf("float8", encodingArray);
                statement.setArray(5, faceEncoding);
            }
            else {
                statement.setArray(5, null);
            }
            
            // Thực thi câu lệnh INSERT
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) 
                System.out.println("Insert success!!");
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        UserController test = new UserController();
        test.getListUserDatabase();

        // test.insertUser(new User("12345", "Dương Đình Lực", null, null, null));
        
        for (User user : test.getUserList()) {
            System.out.println(user.toString());
        }
        
    }
}
