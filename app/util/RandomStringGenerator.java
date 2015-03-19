package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import models.User;
import play.db.DB;

public class RandomStringGenerator {
	
	public static String replaceProfPicName() {
		String name = "";
		int exists = 0;
		do {
			name = generateRandom();
			exists = User.checkIfExists("profile_picture", name);
		} while(exists != 0);
		return name;
	}
	
	public static String replacePostPicName() {
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String name = "";
		boolean duplicateName = true;
		try {
			name = generateRandom();
			do {
				ps = con.prepareStatement("select * from squibs where pic = ?");
				ps.setString(1, name);
				rs = ps.executeQuery();
				if(rs.next())
					name = generateRandom();
				else 
					duplicateName = false;
			} while(duplicateName);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close(); 
				ps.close(); 
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return name;
	}
	
	public static String generateRandom() {
		final String randString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		Random random = new Random();
		
		StringBuilder sb = new StringBuilder(6);
		for(int i = 0; i < 6; i++)
			sb.append(randString.charAt(random.nextInt(randString.length())));
		sb.append(".jpg");
		return(sb.toString());
	}
}
