package util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import play.Logger;

public class DatabaseManager {

	public static void close(ResultSet rs, Statement ps, Connection con) {
	    if (rs != null) {
	        try {
	            rs.close();
	        } catch (SQLException e) {
	        	Logger.error("The result set cannot be closed: " + e.getMessage());
	            System.out.println("The result set cannot be closed: " + e.getMessage());
	        }
	    }
	    if (ps != null) {
	        try {
	            ps.close();
	        } catch (SQLException e) {
	        	Logger.error("The statement cannot be closed: " + e.getMessage());
	            System.out.println("The statement cannot be closed: " + e.getMessage());
	        }
	    }
	    if (con != null) {
	        try {
	            con.close();
	        } catch (SQLException e) {
	        	Logger.error("The data source connection cannot be closed: " + e.getMessage());
	            System.out.println("The data source connection cannot be closed: " + e.getMessage());
	        }
	    }
	}
}
