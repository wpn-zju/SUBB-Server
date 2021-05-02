package com.subb.service.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.subb.service.controller.enums.*;
import com.subb.service.database.exception.*;
import com.subb.service.database.model.account.ContactData;
import com.subb.service.database.model.account.UserData;
import com.subb.service.database.model.site.CommentData;
import com.subb.service.database.model.site.ForumData;
import com.subb.service.database.model.site.PostData;
import com.subb.service.database.model.site.ThreadData;
import com.subb.service.tool.JsonObject;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final String url = "jdbc:MySQL://localhost:3306/subb_prod?" +
            "useUnicode=true&" +
            "characterEncoding=utf-8&" +
            "serverTimezone=UTC&" +
            "useSSL=false&" +
            "allowPublicKeyRetrieval=true";
    private static final String user = "zjuwpn";
    private static final String password = "peinan";
    private static final String defaultAvatarLink = "https://peinanweng.com/download_index/base/avatar.png";

    private static final String queryUserIdByEmail = "select user_id from account where user_email = ?";
    public static boolean hasUserWithEmail(String userEmail) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryUserIdByEmailSt = con.prepareStatement(queryUserIdByEmail)) {
            queryUserIdByEmailSt.setString(1, userEmail);
            try (ResultSet rs = queryUserIdByEmailSt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed hasUserWithEmail(String userEmail)");
        }
    }

    public static int queryUserIdByEmail(String userEmail) throws UserEmailNotExistsException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryUserIdByEmailSt = con.prepareStatement(queryUserIdByEmail)) {
            queryUserIdByEmailSt.setString(1, userEmail);
            try (ResultSet rs = queryUserIdByEmailSt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new UserEmailNotExistsException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed queryUserIdByEmail(String userEmail)");
        }
    }

    private static final String setSessionStatus = "update session set session_status = ? where session_id = ?";
    private static void setSessionStatus(String session, String newStatus) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement setSessionStatusSt = con.prepareStatement(setSessionStatus)) {
            setSessionStatusSt.setString(1, newStatus);
            setSessionStatusSt.setString(2, session);
            setSessionStatusSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed setSessionStatus(String session, String newStatus)");
        }
    }

    private static final String querySession = "select " +
            "session_id, session_user_id, session_create_datetime, session_expire_datetime, session_status " +
            "from session where session_id = ?";
    @SuppressWarnings("unused")
    public static int queryUserIdBySession(String sessionToken)
            throws SessionInvalidException, SessionExpiredException, SessionRevokedException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryUserIdBySessionSt = con.prepareStatement(querySession)) {
            queryUserIdBySessionSt.setString(1, sessionToken);
            try (ResultSet rs = queryUserIdBySessionSt.executeQuery()) {
                if (rs.next()) {
                    String sessionId = rs.getString(1);
                    int sessionUserId = rs.getInt(2);
                    Instant sessionCreateTime = rs.getTimestamp(3).toInstant();
                    Instant sessionExpireTime = rs.getTimestamp(4).toInstant();
                    String sessionStatus = rs.getString(5);
                    if (sessionStatus.equals(EnumSessionStatus.SESSION_STATUS_VALID.toString())) {
                        if (sessionExpireTime.isBefore(Instant.now())) {
                            setSessionStatus(sessionId, EnumSessionStatus.SESSION_STATUS_EXPIRED.toString());
                            throw new SessionExpiredException();
                        } else {
                            return sessionUserId;
                        }
                    } else if (sessionStatus.equals(EnumSessionStatus.SESSION_STATUS_EXPIRED.toString())) {
                        throw new SessionExpiredException();
                    } else if (sessionStatus.equals(EnumSessionStatus.SESSION_STATUS_REVOKED.toString())) {
                        throw new SessionRevokedException();
                    } else {
                        throw new SessionInvalidException();
                    }
                } else {
                    throw new SessionInvalidException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed queryUserIdBySession(String sessionToken)");
        }
    }

    /* Create an new account, have to manually set user_name and user_password after the account has created. */
    private static final String insertAccount = "insert into user_main " +
            "(user_email, user_name, user_password_hash) " +
            "values (?, 'New User', 'password_placeholder')";
    private static final String insertAccountDetail = "insert into account_detail " +
            "(user_id, user_gender, user_avatar_link, user_personal_info, user_posts, user_exp, user_prestige) " +
            "values (?, 0, ?, 'Hello World!', 0, 0, 0)";
    private static final String queryLastInserted = "select last_insert_id()";
    public static int newAccount(String userEmail) throws UserEmailExistsException {
        if (hasUserWithEmail(userEmail)) { throw new UserEmailExistsException(); }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertAccountSt = con.prepareStatement(insertAccount);
             PreparedStatement insertAccountDetailSt = con.prepareStatement(insertAccountDetail);
             PreparedStatement queryLastInsertedSt = con.prepareStatement(queryLastInserted)) {
            insertAccountSt.setString(1, userEmail);
            insertAccountSt.executeUpdate();
            try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt(1);
                    insertAccountDetailSt.setInt(1, userId);
                    insertAccountDetailSt.setString(2, defaultAvatarLink);
                    insertAccountDetailSt.executeUpdate();
                    return userId;
                } else {
                    throw new DataAccessException("Unexpected Error - Create New Account!");
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newAccount(String userEmail)");
        }
    }

    private static final String updateUserName = "update user_main set user_name = ? where user_id = ?";
    public static void modifyUserName(int userId, String newName) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement updateUserNameSt = con.prepareStatement(updateUserName)) {
            updateUserNameSt.setString(1, newName);
            updateUserNameSt.setInt(2, userId);
            updateUserNameSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyUserName(int userId, String newName)");
        }
    }

    public static String passwordHash(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        return Hex.encodeHexString(md.digest()).toLowerCase(Locale.ROOT);
    }

    private static final String setUserAllSessionStatus = "update session set session_status = ? where session_user_id = ?";
    private static final String updateUserPassword = "update user_main set user_password_hash = ? where user_id = ?";
    public static void modifyUserPassword(int userId, String newPassword) {
        try (Connection con = DriverManager.getConnection(url, user, newPassword);
             PreparedStatement setSessionStatusUserAllSt = con.prepareStatement(setUserAllSessionStatus);
             PreparedStatement updateUserPasswordSt = con.prepareStatement(updateUserPassword)) {
            String hash = passwordHash(password);
            setSessionStatusUserAllSt.setString(1, EnumSessionStatus.SESSION_STATUS_REVOKED.toString());
            setSessionStatusUserAllSt.setInt(2, userId);
            setSessionStatusUserAllSt.executeUpdate();
            updateUserPasswordSt.setString(1, hash);
            updateUserPasswordSt.setInt(2, userId);
            updateUserPasswordSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyUserPassword(int userId, String newPassword)");
        } catch (NoSuchAlgorithmException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - Hash algorithm not found");
        }
    }

    // Gender
    // 0 - Hide
    // 1 - Male
    // 2 - Female
    // 3 - Others
    private static final String updateUserGender = "update user_detail set user_gender = ? where user_id = ?";
    public static void modifyUserGender(int userId, int newGender) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement updateUserGenderSt = con.prepareStatement(updateUserGender)) {
            updateUserGenderSt.setInt(1, newGender);
            updateUserGenderSt.setInt(2, userId);
            updateUserGenderSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyUserGender(int userId, int newGender)");
        }
    }

    private static final String updateUserAvatarLink = "update user_detail set user_avatar_link = ? where user_id = ?";
    public static void modifyUserAvatarLink(int userId, String newAvatarLink) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement updateUserAvatarLinkSt = con.prepareStatement(updateUserAvatarLink)) {
            updateUserAvatarLinkSt.setString(1, newAvatarLink);
            updateUserAvatarLinkSt.setInt(2, userId);
            updateUserAvatarLinkSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyUserAvatarLink(int userId, String newAvatarLink)");
        }
    }

    private static final String updateUserPersonalInfo = "update user set user_personal_info = ? where user_id = ?";
    public static void modifyUserPersonalInfo(int userId, String newInfo) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement updateUserInfoSt = con.prepareStatement(updateUserPersonalInfo)) {
            updateUserInfoSt.setString(1, newInfo);
            updateUserInfoSt.setInt(2, userId);
            updateUserInfoSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyUserInfo(int userId, String newInfo)");
        }
    }

    private static final String updateLoginTimestamp = "update user_main set user_login_timestamp = ? where user_id = ?";
    private static void updateLoginRecord(int userId) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement updateLoginTimestampSt = con.prepareStatement(updateLoginTimestamp)) {
            updateLoginTimestampSt.setTimestamp(1, Timestamp.from(Instant.now()));
            updateLoginTimestampSt.setInt(2, userId);
            updateLoginTimestampSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed updateLoginRecord(int userId)");
        }
    }

    private static final String queryUser = "select " +
            "p1.user_id, p1.user_email, p1.user_name, p1.user_password_hash, p1.user_privilege " +
            "p2.user_gender, p2.user_avatar_link, p2.user_personal_info, p2.user_posts, p2.user_exp, p2.user_prestige " +
            "from user_main p1 inner join " +
            "(select user_id, user_gender, user_avatar_link, user_personal_info, user_posts, user_exp, user_prestige from user_detail) p2 " +
            "on p1.user_id = p2.user_id where p1.user_id = ?";
    public static UserData getUserDataAll(int userId) throws UserNotExistsException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryUserSt = con.prepareStatement(queryUser)) {
            queryUserSt.setInt(1, userId);
            try (ResultSet rs = queryUserSt.executeQuery()) {
                if (rs.next()) {
                    return UserData.builder()
                            .userId(rs.getInt(1))
                            .email(rs.getString(2))
                            .nickname(rs.getString(3))
                            .passwordHash(rs.getString(4))
                            .privilege(EnumUserPrivilege.valueOf(rs.getString(5)))
                            .gender(rs.getInt(6))
                            .avatarLink(rs.getString(7))
                            .personalInfo(rs.getString(8))
                            .posts(rs.getInt(9))
                            .exp(rs.getInt(10))
                            .prestige(rs.getInt(11)).build();
                } else {
                    throw new UserNotExistsException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getUserDataAll(int userId)");
        }
    }

    public static ContactData getUserData(int contactId) throws UserNotExistsException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryUserSt = con.prepareStatement(queryUser)) {
            queryUserSt.setInt(1, contactId);
            try (ResultSet rs = queryUserSt.executeQuery()) {
                if (rs.next()) {
                    return ContactData.builder()
                            .contactId(rs.getInt(1))
                            .email(rs.getString(2))
                            .nickname(rs.getString(3))
                            .privilege(EnumUserPrivilege.valueOf(rs.getString(5)))
                            .gender(rs.getInt(6))
                            .avatarLink(rs.getString(7))
                            .personalInfo(rs.getString(8))
                            .posts(rs.getInt(9))
                            .exp(rs.getInt(10))
                            .prestige(rs.getInt(11)).build();
                } else {
                    throw new UserNotExistsException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getUserData(int contactId)");
        }
    }

    private static final String queryBrowsingHistory = "select record_id, record_user_id, record_thread_id, record_timestamp " +
            "from browsing_history where record_user_id = ?";
    public static JsonObject getBrowsingHistory(int userId)  {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryBrowsingHistorySt = con.prepareStatement(queryBrowsingHistory)) {
            queryBrowsingHistorySt.setInt(1, userId);
            try (ResultSet rs = queryBrowsingHistorySt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    JsonObject record = new JsonObject(new LinkedHashMap<>());
                    record.put("user_id", new JsonObject(rs.getInt(2)));
                    record.put("thread_id", new JsonObject(rs.getInt(3)));
                    record.put("timestamp", new JsonObject(rs.getTimestamp(4).toInstant().toString()));
                    result.add(record);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getBrowsingHistory(int userId)");
        }
    }

    private static final int historyRecordsPerPage = 30;
    private static final String queryThreadHistory = "select thread_id " +
            "from thread where thread_author = ? limit ?, ?";
    public static JsonObject getThreadHistory(int userId, int page) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryThreadHistorySt = con.prepareStatement(queryThreadHistory)) {
            queryThreadHistorySt.setInt(1, userId);
            queryThreadHistorySt.setInt(2, historyRecordsPerPage * page);
            queryThreadHistorySt.setInt(3, historyRecordsPerPage);
            try (ResultSet rs = queryThreadHistorySt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int threadId = rs.getInt(1);
                    ThreadData threadData = getThreadData(threadId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(threadData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getThreadHistory(int userId, int page)");
        } catch (ThreadNotExistException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get thread history");
        }
    }

    private static final String queryPostHistory = "select post_id " +
            "from post where post_author = ? limit ?, ?";
    public static JsonObject getPostHistory(int userId, int page) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryPostHistorySt = con.prepareStatement(queryPostHistory)) {
            queryPostHistorySt.setInt(1, userId);
            queryPostHistorySt.setInt(2, historyRecordsPerPage * page);
            queryPostHistorySt.setInt(3, historyRecordsPerPage);
            try (ResultSet rs = queryPostHistorySt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int postId = rs.getInt(1);
                    PostData postData = getPostData(postId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(postData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getPostHistory(int userId, int page)");
        } catch (PostNotExistsException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get post history");
        }
    }

    private static final String queryCommentHistory = "select comment_id " +
            "from comment where comment_author = ? limit ?, ?";
    private static JsonObject getCommentHistory(int userId, int page) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryCommentHistorySt = con.prepareStatement(queryCommentHistory)) {
            queryCommentHistorySt.setInt(1, userId);
            queryCommentHistorySt.setInt(2, historyRecordsPerPage * page);
            queryCommentHistorySt.setInt(3, historyRecordsPerPage);
            try (ResultSet rs = queryCommentHistorySt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int commentId = rs.getInt(1);
                    CommentData commentData = getCommentData(commentId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(commentData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getCommentHistory(int userId, int page)");
        } catch (CommentNotExistsException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get comment history");
        }
    }

    private static final String queryHomepage = "select thread_id from thread where thread_create_timestamp > ? order by thread_heat desc limit 10";
    public static JsonObject getHomepage() {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryHomepageSt = con.prepareStatement(queryHomepage)) {
            queryHomepageSt.setTimestamp(1, Timestamp.from(Instant.now()));
            try (ResultSet rs = queryHomepageSt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int threadId = rs.getInt(1);
                    ThreadData threadData = getThreadData(threadId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(threadData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getHomepage()");
        } catch (ThreadNotExistException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get homepage");
        }
    }

    // Todo: Caches for get page requests
    // Todo: Counting heat, exp and posts
    private static final int threadsPerPage = 30;
    private static final String getForumPage = "select thread_id from thread where thread_forum_id = ? order by thread_active_timestamp desc limit ?, ?";
    public static JsonObject getForumPage(int forumId, int page) throws MalformedRequestException {
        try {
            ForumData forumData = getForumData(forumId);
        } catch (ForumNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getForumPageSt = con.prepareStatement(getForumPage)) {
            getForumPageSt.setInt(1, forumId);
            getForumPageSt.setInt(2, threadsPerPage * page);
            getForumPageSt.setInt(3, threadsPerPage);
            try (ResultSet rs = getForumPageSt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int threadId = rs.getInt(1);
                    ThreadData threadData = getThreadData(threadId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(threadData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getForumPage(int forumId, int page)");
        } catch (ThreadNotExistException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get forum page");
        }
    }

    private static final int postsPerPage = 30;
    private static final String getThreadPage = "select post_id from post where post_thread_id = ? limit ?, ?";
    public static JsonObject getThreadPage(int threadId, int page) throws MalformedRequestException {
        try {
            ThreadData threadData = getThreadData(threadId);
        } catch (ThreadNotExistException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getThreadPageSt = con.prepareStatement(getThreadPage)) {
            getThreadPageSt.setInt(1, threadId);
            getThreadPageSt.setInt(2, postsPerPage * page);
            getThreadPageSt.setInt(3, postsPerPage);
            try (ResultSet rs = getThreadPageSt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int postId = rs.getInt(1);
                    PostData postData = getPostData(postId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(postData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getThreadPage(int threadId, int page)");
        } catch (PostNotExistsException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get thread page");
        }
    }

    private static final int commentsPerPage = 30;
    private static final String getPostPage = "select comment_id from comment where comment_post_id = ? limit ?, ?";
    public static JsonObject getPostPage(int postId, int page) throws MalformedRequestException {
        try {
            PostData postData = getPostData(postId);
        } catch (PostNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getPostPageSt = con.prepareStatement(getPostPage)) {
            getPostPageSt.setInt(1, postId);
            getPostPageSt.setInt(2, commentsPerPage * page);
            getPostPageSt.setInt(3, commentsPerPage);
            try (ResultSet rs = getPostPageSt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int commentId = rs.getInt(1);
                    CommentData commentData = getCommentData(commentId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(commentData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getPostPage(int postId, int page)");
        } catch (CommentNotExistsException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get post page");
        }
    }

    private static final String getCommentPage = "select comment_id from comment where comment_root_id = ? limit ?, ?";
    public static JsonObject getCommentPage(int commentId, int page) throws MalformedRequestException {
        try {
            CommentData commentData = getCommentData(commentId);
        } catch (CommentNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getCommentPageSt = con.prepareStatement(getCommentPage)) {
            getCommentPageSt.setInt(1, commentId);
            getCommentPageSt.setInt(2, threadsPerPage * page);
            getCommentPageSt.setInt(3, threadsPerPage);
            try (ResultSet rs = getCommentPageSt.executeQuery()) {
                JsonObject result = new JsonObject(new ArrayList<>());
                while (rs.next()) {
                    int childCommentId = rs.getInt(1);
                    CommentData childCommentData = getCommentData(childCommentId);
                    result.add(new JsonObject(new ObjectMapper().writeValueAsString(childCommentData)));
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getCommentPage(int commentId, int page)");
        } catch (CommentNotExistsException | JsonProcessingException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get comment page");
        }
    }

    private static final String queryForum = "select " +
            "forum_id, forum_title, forum_posts, forum_heat from forum where forum_id = ?";
    private static final String queryThread = "select " +
            "thread_id, thread_forum_id, thread_title, thread_author, thread_create_timestamp, thread_active_timestamp, thread_status, thread_posts, thread_views, thread_votes, thread_heat,  from thread where thread_id = ?";
    private static final String queryPost = "select " +
            "post_id, post_thread_id, post_author, post_timestamp, post_refer_to, post_content, post_status, post_comments, post_votes from post where post_id = ?";
    private static final String queryComment = "select " +
            "comment_id, comment_post_id, comment_root_id, comment_author, comment_timestamp, comment_refer_to, comment_content, comment_status, comment_comments, comment_votes, from comment where comment_id = ?";
    public static ForumData getForumData(int forumId) throws ForumNotExistsException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryForumSt = con.prepareStatement(queryForum)) {
            queryForumSt.setInt(1, forumId);
            try (ResultSet rs = queryForumSt.executeQuery()) {
                if (!rs.next()) {
                    return ForumData.builder()
                            .forumId(rs.getInt(1))
                            .title(rs.getString(2))
                            .posts(rs.getInt(3))
                            .heat(rs.getInt(4))
                            .build();
                } else {
                    throw new ForumNotExistsException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getForumData(int forumId)");
        }
    }

    public static ThreadData getThreadData(int threadId) throws ThreadNotExistException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryThreadSt = con.prepareStatement(queryThread)) {
            queryThreadSt.setInt(1, threadId);
            try (ResultSet rs = queryThreadSt.executeQuery()) {
                if (rs.next()) {
                    return ThreadData.builder()
                            .threadId(rs.getInt(1))
                            .forumId(rs.getInt(2))
                            .title(rs.getString(3))
                            .author(rs.getInt(4))
                            .createTimestamp(rs.getTimestamp(5).toInstant())
                            .activeTimestamp(rs.getTimestamp(6).toInstant())
                            .status(EnumThreadStatus.valueOf(rs.getString(7)))
                            .posts(rs.getInt(7))
                            .views(rs.getInt(8))
                            .votes(rs.getInt(9))
                            .heat(rs.getInt(10)).build();
                } else {
                    throw new ThreadNotExistException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getThreadData(int threadId)");
        }
    }

    public static PostData getPostData(int postId) throws PostNotExistsException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryPostSt = con.prepareStatement(queryPost)) {
            queryPostSt.setInt(1, postId);
            try (ResultSet rs = queryPostSt.executeQuery()) {
                if (rs.next()) {
                    return PostData.builder()
                            .postId(rs.getInt(1))
                            .threadId(rs.getInt(2))
                            .author(rs.getInt(3))
                            .timestamp(rs.getTimestamp(4).toInstant())
                            .referTo(rs.getInt(5))
                            .content(rs.getString(6))
                            .status(EnumPostStatus.valueOf(rs.getString(7)))
                            .comments(rs.getInt(8))
                            .votes(rs.getInt(9))
                            .build();
                } else {
                    throw new PostNotExistsException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getPostData(int postId)");
        }
    }

    public static CommentData getCommentData(int commentId) throws CommentNotExistsException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryCommentSt = con.prepareStatement(queryComment)) {
            queryCommentSt.setInt(1, commentId);
            try (ResultSet rs = queryCommentSt.executeQuery()) {
                if (rs.next()) {
                    return CommentData.builder()
                            .commentId(rs.getInt(1))
                            .postId(rs.getInt(2))
                            .rootId(rs.getInt(3))
                            .author(rs.getInt(4))
                            .timestamp(rs.getTimestamp(5).toInstant())
                            .referTo(rs.getInt(6))
                            .content(rs.getString(7))
                            .status(EnumCommentStatus.valueOf(rs.getString(8)))
                            .comments(rs.getInt(9))
                            .votes(rs.getInt(10))
                            .build();
                } else {
                    throw new CommentNotExistsException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getCommentData(int commentId)");
        }
    }

    private static final String insertMessage = "insert into message (message_sender, message_receiver, message_type, message_status, message_content) " +
            "values (?, ?, ?, ?, ?)";
    public static void pushOfflineMessage(int sender, int receiver, String content, EnumMessageType messageType) throws UserNotExistsException {
        ContactData receiverData = getUserData(receiver);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertMessageSt = con.prepareStatement(insertMessage)) {
            insertMessageSt.setInt(1, sender);
            insertMessageSt.setInt(2, receiver);
            insertMessageSt.setString(3, messageType.toString());
            insertMessageSt.setString(4, EnumMessageStatus.MESSAGE_STATUS_PENDING.toString());
            insertMessageSt.setString(5, content);
            insertMessageSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed pushOfflineMessage(int userId, String content, EnumMessageType messageType)");
        }
    }

    private static final String queryUserMessageList = "select message_id, message_sender, message_receiver, message_type, message_status, message_content " +
            "from account where message_receiver = ?";
    private static final String setMessageStatus = "update message set message_status = ? where message_id = ?";
    public static JsonObject popOfflineMessageAsList(int userId) {
        List<JsonObject> result = new ArrayList<>();
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryUserMessageListSt = con.prepareStatement(queryUserMessageList);
             PreparedStatement setMessageStatusSt = con.prepareStatement(setMessageStatus)) {
            queryUserMessageListSt.setInt(1, userId);
            try (ResultSet rs = queryUserMessageListSt.executeQuery()) {
                if (rs.next()) {
                    JsonObject message = new JsonObject(new LinkedHashMap<>());
                    message.put("message_id", new JsonObject(rs.getInt(1)));
                    message.put("message_sender", new JsonObject(rs.getInt(2)));
                    message.put("message_receiver", new JsonObject(rs.getInt(3)));
                    message.put("message_type", new JsonObject(rs.getString(4)));
                    message.put("message_content", new JsonObject(rs.getString(5)));
                    result.add(message);
                    setMessageStatusSt.setString(1, EnumMessageStatus.MESSAGE_STATUS_POPPED.toString());
                    setMessageStatusSt.setInt(2, rs.getInt(1));
                    setMessageStatusSt.executeUpdate();
                } else {
                    throw new DataAccessException("Unexpected Error - Pop Offline Messages!");
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed popOfflineMessageAsList(int userId)");
        }

        return new JsonObject(result);
    }

    private static final String passcodeCharSet = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static String generateNewPasscode() {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 6; ++i) {
            int random = r.nextInt();
            int index = random % passcodeCharSet.length();
            if (index < 0) index = index + passcodeCharSet.length();
            sb.append(passcodeCharSet.charAt(index));
        }
        logger.info(String.format("New Passcode -> %s", sb.toString()));
        return sb.toString();
    }

    private static final Duration PASSCODE_EXPIRE_DURATION = Duration.ofMinutes(10);
    private static final String insertPasscode = "insert into passcode " +
            "(passcode, passcode_email, passcode_create_datetime, passcode_expire_datetime, passcode_status) " +
            "values (?, ?, ?, ?, ?)";
    public static String newPasscode(String email) {
        try (Connection con = DriverManager.getConnection(url, user, password);
            PreparedStatement insertPasscodeSt = con.prepareStatement(insertPasscode)) {
            String passcode = generateNewPasscode();
            insertPasscodeSt.setString(1, passcode);
            insertPasscodeSt.setString(2, email);
            insertPasscodeSt.setTimestamp(3, Timestamp.from(Instant.now()));
            insertPasscodeSt.setTimestamp(4, Timestamp.from(Instant.now().plus(PASSCODE_EXPIRE_DURATION)));
            insertPasscodeSt.setString(5, EnumPasscodeStatus.PASSCODE_STATUS_PENDING.toString());
            insertPasscodeSt.executeUpdate();
            return passcode;
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newPasscode(String email)");
        }
    }

    private static final String setPasscodeStatus = "update passcode set passcode_status = ? where passcode = ?";
    private static void setPasscodeStatus(String passcode, String newStatus) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement setPasscodeStatusSt = con.prepareStatement(setPasscodeStatus)) {
            setPasscodeStatusSt.setString(1, newStatus);
            setPasscodeStatusSt.setString(2, passcode);
            setPasscodeStatusSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed setPasscodeStatus(String passcode, String newStatus)");
        }
    }

    private static final String queryPasscode = "select " +
            "passcode_email, passcode_create_datetime, passcode_expire_datetime, passcode_status " +
            "from passcode where passcode = ?";
    @SuppressWarnings("unused")
    public static void validatePasscode(String passcode, String email) throws PasscodeException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryPasscodeSt = con.prepareStatement(queryPasscode)) {
            queryPasscodeSt.setString(1, passcode);
            try (ResultSet rs = queryPasscodeSt.executeQuery()) {
                if (rs.next()) {
                    String passcodeEmail = rs.getString(1);
                    Instant passcodeCreateTime = rs.getTimestamp(2).toInstant();
                    Instant passcodeExpireTime = rs.getTimestamp(3).toInstant();
                    String passcodeStatus = rs.getString(4);
                    if (passcodeStatus.equals(EnumPasscodeStatus.PASSCODE_STATUS_PENDING.toString())) {
                        if (passcodeExpireTime.isBefore(Instant.now())) {
                            setPasscodeStatus(passcode, EnumPasscodeStatus.PASSCODE_STATUS_EXPIRED.toString());
                            throw new PasscodeException();
                        } else if (!passcodeEmail.equals(email)) {
                            throw new PasscodeException();
                        }
                    } else {
                        throw new PasscodeException();
                    }
                } else {
                    throw new PasscodeException();
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed String passcode, String email");
        }
    }

    private static final Duration SESSION_EXPIRE_DURATION = Duration.ofDays(30);
    private static final String insertSession = "insert into session " +
            "(session_id, session_user_id, session_create_datetime, session_expire_datetime, session_status) " +
            "values (?, ?, ?, ?, ?)";
    public static void insertSession(int userId, String session) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertSessionSt = con.prepareStatement(insertSession)) {
            insertSessionSt.setString(1, session);
            insertSessionSt.setInt(2, userId);
            insertSessionSt.setTimestamp(3, Timestamp.from(Instant.now()));
            insertSessionSt.setTimestamp(4, Timestamp.from(Instant.now().plus(SESSION_EXPIRE_DURATION)));
            insertSessionSt.setString(5, EnumSessionStatus.SESSION_STATUS_VALID.toString());
            insertSessionSt.executeUpdate();
            updateLoginRecord(userId);
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed insertSession(int userId, String session)");
        }
    }

    private static final Duration FILE_EXPIRE_DURATION = Duration.ofDays(365);
    private static final String insertFileArchive = "insert into file " +
            "(file_name, file_link, file_uploader, file_upload_time, file_size) " +
            "values (?, ?, ?, ?, ?)";
    public static void newFileDescriptor(String fileName, String fileLink, int fileUploader, int fileSize) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertFileArchiveSt = con.prepareStatement(insertFileArchive)) {
            insertFileArchiveSt.setString(1, fileName);
            insertFileArchiveSt.setString(2, fileLink);
            insertFileArchiveSt.setInt(3, fileUploader);
            insertFileArchiveSt.setTimestamp(4, Timestamp.from(Instant.now()));
            insertFileArchiveSt.setTimestamp(5, Timestamp.from(Instant.now().plus(FILE_EXPIRE_DURATION)));
            insertFileArchiveSt.setInt(6, fileSize);
            insertFileArchiveSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newFileDescriptor(String fileName, String fileLink, int fileUploader, int fileSize)");
        }
    }

    private static final String insertThread = "insert into thread " +
            "(thread_forum_id, thread_title, thread_author, thread_create_timestamp, thread_active_timestamp, thread_status, thread_posts, thread_views, thread_votes, thread_heat) " +
            "values (?, ?, ?, ?, ?, ?, 0, 0, 0, 0)";
    public static int newThread(int authorId, int forumId, String threadTitle) throws MalformedRequestException {
        try {
            ForumData forumData = getForumData(forumId);
        } catch (ForumNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertThreadSt = con.prepareStatement(insertThread);
             PreparedStatement queryLastInsertedSt = con.prepareStatement(queryLastInserted)) {
            insertThreadSt.setInt(1, forumId);
            insertThreadSt.setString(2, threadTitle);
            insertThreadSt.setInt(3, authorId);
            insertThreadSt.setTimestamp(4, Timestamp.from(Instant.now()));
            insertThreadSt.setTimestamp(5, Timestamp.from(Instant.now()));
            insertThreadSt.setString(6, EnumThreadStatus.THREAD_STATUS_VISIBLE.toString());
            insertThreadSt.executeUpdate();
            try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DataAccessException("Unexpected Error - Create New Thread!");
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newThread(int authorId, int forumId, String threadTitle, String threadContent)");
        }
    }

    private static final String insertPost = "insert into post " +
            "(post_thread_id, post_author, post_timestamp, post_refer_to, post_content, post_status, post_comments, post_votes) " +
            "values (?, ?, ?, ?, ?, ?, 0, 0)";
    public static void newPost(int authorId, int threadId, int referTo, String postContent) throws MalformedRequestException {
        try {
            ThreadData threadData = getThreadData(threadId);
            if (referTo != 0 && getPostData(referTo).getThreadId() != threadId) {
                throw new MalformedRequestException();
            }
        } catch (ThreadNotExistException | PostNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertPostSt = con.prepareStatement(insertPost)) {
            insertPostSt.setInt(1, threadId);
            insertPostSt.setInt(2, authorId);
            insertPostSt.setTimestamp(3, Timestamp.from(Instant.now()));
            insertPostSt.setInt(4, referTo);
            insertPostSt.setString(5, postContent);
            insertPostSt.setString(6, EnumPostStatus.POST_STATUS_VISIBLE.toString());
            insertPostSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newPost(int authorId, int threadId, int referTo, String postContent)");
        }
    }

    private static final String insertComment = "insert into comment " +
            "(comment_post_id, comment_root_id, comment_author, comment_timestamp, comment_refer_to, comment_content, comment_status, comment_comments, comment_votes) " +
            "values (?, ?, ?, ?, ?, ?, ?, 0, 0)";
    public static void newComment(int authorId, int postId, int referTo, String commentContent) throws MalformedRequestException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertCommentSt = con.prepareStatement(insertComment)) {
            PostData postData = getPostData(postId);
            if (referTo == 0) {
                insertCommentSt.setInt(2, 0);
            } else {
                CommentData rootCommentData = getCommentData(referTo);
                if (rootCommentData.getPostId() != postId) {
                    throw new MalformedRequestException();
                }
                if (rootCommentData.getRootId() == 0) {
                    insertCommentSt.setInt(2, rootCommentData.getCommentId());
                } else {
                    insertCommentSt.setInt(2, rootCommentData.getRootId());
                }
            }
            insertCommentSt.setInt(1, postId);
            insertCommentSt.setInt(3, authorId);
            insertCommentSt.setTimestamp(4, Timestamp.from(Instant.now()));
            insertCommentSt.setInt(5, referTo);
            insertCommentSt.setString(6, commentContent);
            insertCommentSt.setString(7, EnumCommentStatus.COMMENT_STATUS_VISIBLE.toString());
            insertCommentSt.executeUpdate();
        } catch (PostNotExistsException | CommentNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newComment(int authorId, int postId, int referTo, String commentContent)");
        }
    }

    private static final String setThreadStatus = "update thread set thread_status = ? where thread_id = ?";
    public static void deleteThread(int userId, int threadId) throws MalformedRequestException, OperationForbiddenException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement deleteThreadSt = con.prepareStatement(setThreadStatus)) {
            ThreadData threadData = getThreadData(threadId);
            if (threadData.getAuthor() != userId) {
                throw new OperationForbiddenException();
            }
            deleteThreadSt.setInt(1, threadId);
            deleteThreadSt.setString(2, EnumThreadStatus.THREAD_STATUS_DELETED.toString());
            deleteThreadSt.executeUpdate();
        } catch (ThreadNotExistException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed deleteThread(int userId, int threadId)");
        }
    }

    private static final String setPostStatus = "update post set post_status = ? where post_id = ?";
    public static void deletePost(int userId, int postId) throws MalformedRequestException, OperationForbiddenException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement deletePostSt = con.prepareStatement(setPostStatus)) {
            PostData postData = getPostData(postId);
            ThreadData threadData = getThreadData(postData.getThreadId());
            if (postData.getAuthor() != userId
                    && threadData.getAuthor() != userId) {
                throw new OperationForbiddenException();
            }
            deletePostSt.setInt(1, postId);
            deletePostSt.setString(2, EnumPostStatus.POST_STATUS_DELETED.toString());
            deletePostSt.executeUpdate();
        } catch (ThreadNotExistException | PostNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed deletePost(int userId, int postId)");
        }
    }

    private static final String setCommentStatus = "update thread set comment_status = ? where comment_id = ?";
    public static void deleteComment(int userId, int commentId) throws MalformedRequestException, OperationForbiddenException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement deleteCommentSt = con.prepareStatement(setCommentStatus)) {
            CommentData commentData = getCommentData(commentId);
            PostData postData = getPostData(commentData.getPostId());
            ThreadData threadData = getThreadData(postData.getThreadId());
            if (commentData.getAuthor() != userId
                    && postData.getAuthor() != userId
                    && threadData.getAuthor() != userId
                    && (commentData.getRootId() == 0 || getCommentData(commentData.getRootId()).getAuthor() != userId)) {
                throw new OperationForbiddenException();
            }
            deleteCommentSt.setInt(1, commentId);
            deleteCommentSt.setString(2, EnumCommentStatus.COMMENT_STATUS_DELETED.toString());
            deleteCommentSt.executeUpdate();
        } catch (ThreadNotExistException | PostNotExistsException | CommentNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed deleteComment(int userId, int commentId)");
        }
    }

    private static final String checkThreadVoted = "select record_id from thread_vote where record_thread_id = ? and record_voter_id = ?";
    private static boolean hasVotedThread(int voterId, int threadId) throws ThreadNotExistException {
        ThreadData threadData = getThreadData(threadId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement checkThreadVotedSt = con.prepareStatement(checkThreadVoted)) {
            checkThreadVotedSt.setInt(1, threadId);
            checkThreadVotedSt.setInt(2, voterId);
            try (ResultSet rs = checkThreadVotedSt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed hasVotedThread(int voterId, int threadId)");
        }
    }

    private static final String checkPostVoted = "select record_id from post_vote where record_post_id = ? and record_voter_id = ?";
    private static boolean hasVotedPost(int voterId, int postId) throws PostNotExistsException {
        PostData postData = getPostData(postId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement checkPostVotedSt = con.prepareStatement(checkPostVoted)) {
            checkPostVotedSt.setInt(1, postId);
            checkPostVotedSt.setInt(2, voterId);
            try (ResultSet rs = checkPostVotedSt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed hasVotedPost(int voterId, int postId)");
        }
    }

    private static final String checkCommentVoted = "select record_id from comment_vote where record_comment_id = ? and record_voter_id = ?";
    private static boolean hasVotedComment(int voterId, int commentId) throws CommentNotExistsException {
        CommentData commentData = getCommentData(commentId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement checkCommentVotedSt = con.prepareStatement(checkCommentVoted)) {
            checkCommentVotedSt.setInt(1, commentId);
            checkCommentVotedSt.setInt(2, voterId);
            try (ResultSet rs = checkCommentVotedSt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed hasVotedComment(int voterId, int commentId)");
        }
    }

    private static final String voteThread = "insert into thread_vote " +
            "(record_thread_id, record_voter_id) " +
            "values (?, ?)";
    private static final String deleteVoteRecordThread = "delete from thread_vote where record_thread_id = ? and record_voter_id = ?";
    private static final String voteThreadUpdate = "update thread set thread_votes = thread_votes + ? where thread_id = ?";
    public static void voteThread(int voterId, int threadId) throws ThreadNotExistException {
        ThreadData threadData = getThreadData(threadId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement voteThreadSt = con.prepareStatement(voteThread);
             PreparedStatement deleteVoteRecordSt = con.prepareStatement(deleteVoteRecordThread);
             PreparedStatement voteThreadUpdateSt = con.prepareStatement(voteThreadUpdate)) {
            if (hasVotedThread(voterId, threadId)) {
                deleteVoteRecordSt.setInt(1, threadId);
                deleteVoteRecordSt.setInt(2, voterId);
                deleteVoteRecordSt.executeUpdate();
                voteThreadUpdateSt.setInt(1, -1);
            } else {
                voteThreadSt.setInt(1, threadId);
                voteThreadSt.setInt(2, voterId);
                voteThreadSt.executeUpdate();
                voteThreadUpdateSt.setInt(1, 1);
            }
            voteThreadUpdateSt.setInt(2, threadId);
            voteThreadUpdateSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed voteThread(int voterId, int threadId)");
        }
    }

    private static final String votePost = "insert into post_vote " +
            "(record_post_id, record_voter_id) " +
            "values (?, ?)";
    private static final String deleteVoteRecordPost = "delete from post_vote where record_post_id = ? and record_voter_id = ?";
    private static final String votePostUpdate = "update thread set post_votes = post_votes + ? where post_id = ?";
    public static void votePost(int voterId, int postId) throws PostNotExistsException {
        PostData postData = getPostData(postId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement votePostSt = con.prepareStatement(votePost);
             PreparedStatement deleteVoteRecordSt = con.prepareStatement(deleteVoteRecordPost);
             PreparedStatement votePostUpdateSt = con.prepareStatement(votePostUpdate)) {
            if (hasVotedPost(voterId, postId)) {
                deleteVoteRecordSt.setInt(1, postId);
                deleteVoteRecordSt.setInt(2, voterId);
                deleteVoteRecordSt.executeUpdate();
                votePostUpdateSt.setInt(1, -1);
            } else {
                votePostSt.setInt(1, postId);
                votePostSt.setInt(2, voterId);
                votePostSt.executeUpdate();
                votePostUpdateSt.setInt(1, 1);
            }
            votePostUpdateSt.setInt(2, postId);
            votePostUpdateSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed votePost(int voterId, int postId)");
        }
    }

    private static final String voteComment = "insert into comment_vote " +
            "(record_comment_id, record_voter_id) " +
            "values (?, ?)";
    private static final String deleteVoteRecordComment = "delete from comment_vote where record_comment_id = ? and record_voter_id = ?";
    private static final String voteCommentUpdate = "update thread set comment_votes = comment_votes + ? where comment_id = ?";
    public static void voteComment(int voterId, int commentId) throws CommentNotExistsException {
        CommentData commentData = getCommentData(commentId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement voteCommentSt = con.prepareStatement(voteComment);
             PreparedStatement deleteVoteRecordSt = con.prepareStatement(deleteVoteRecordComment);
             PreparedStatement voteCommentUpdateSt = con.prepareStatement(voteCommentUpdate)) {
            if (hasVotedComment(voterId, commentId)) {
                deleteVoteRecordSt.setInt(1, commentId);
                deleteVoteRecordSt.setInt(2, voterId);
                deleteVoteRecordSt.executeUpdate();
                voteCommentUpdateSt.setInt(1, -1);
            } else {
                voteCommentSt.setInt(1, commentId);
                voteCommentSt.setInt(2, voterId);
                voteCommentSt.executeUpdate();
                voteCommentUpdateSt.setInt(1, 1);
            }
            voteCommentUpdateSt.setInt(2, commentId);
            voteCommentUpdateSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed voteComment(int voterId, int commentId)");
        }
    }

    private static void validateAdministrator(int userId) throws UserNotExistsException, OperationForbiddenException {
        if(!getUserData(userId).getPrivilege().equals(EnumUserPrivilege.PRIVILEGE_ADMIN)) {
            throw new OperationForbiddenException();
        }
    }

    // Todo: ADMIN DISABLE ACCOUNT DATABASE DESIGN.
    private static final String setUserPrivilege = "update user_main set user_privilege = ? where user_id = ?";
    public static void adminDisableAccount(int adminId, int userId, int disablePeriod) throws UserNotExistsException, OperationForbiddenException {

    }

    public static void adminDeleteThread(int adminId, int threadId) throws ThreadNotExistException, OperationForbiddenException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement deleteThreadSt = con.prepareStatement(setThreadStatus)) {
            ThreadData threadData = getThreadData(threadId);
            validateAdministrator(adminId);
            deleteThreadSt.setString(1, EnumThreadStatus.THREAD_STATUS_DELETED.toString());
            deleteThreadSt.setInt(2, threadId);
            deleteThreadSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed adminDeleteThread(int adminId, int threadId)");
        } catch (UserNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - admin delete thread");
        }
    }

    public static void adminDeletePost(int adminId, int postId) throws PostNotExistsException, OperationForbiddenException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement deletePostSt = con.prepareStatement(setPostStatus)) {
            PostData postData = getPostData(postId);
            validateAdministrator(adminId);
            deletePostSt.setString(1, EnumPostStatus.POST_STATUS_DELETED.toString());
            deletePostSt.setInt(2, postId);
            deletePostSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed adminDeletePost(int adminId, int postId)");
        } catch (UserNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - admin delete post");
        }
    }

    public static void adminDeleteComment(int adminId, int commentId) throws CommentNotExistsException, OperationForbiddenException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement deleteCommentSt = con.prepareStatement(setCommentStatus)) {
            CommentData commentData = getCommentData(commentId);
            validateAdministrator(adminId);
            deleteCommentSt.setString(1, EnumCommentStatus.COMMENT_STATUS_DELETED.toString());
            deleteCommentSt.setInt(2, commentId);
            deleteCommentSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed adminDeleteComment(int adminId, int commentId)");
        } catch (UserNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - admin delete comment");
        }
    }

    // WARNING - FOR TESTING ONLY
    private static final String deleteUser = "delete from user_main";
    private static final String deleteUserDetail = "delete from user_detail";
    private static final String deleteBrowsingHistory = "delete from browsing_history";
    private static final String deleteThread = "delete from thread";
    private static final String deleteThreadVote = "delete from thread_vote";
    private static final String deletePost = "delete from post";
    private static final String deletePostVote = "delete from post_vote";
    private static final String deleteComment = "delete from comment";
    private static final String deleteCommentVote = "delete from comment_vote";
    private static final String deleteFile = "delete from file";
    private static final String deleteMessage = "delete from message";
    private static final String deletePasscode = "delete from passcode";
    private static final String deleteSession = "delete from session";
    private static final String deleteForum = "delete from forum";
    public static void reset() {
        try (Connection con = DriverManager.getConnection(url, user, password);
            Statement deleteSt = con.createStatement()) {
            deleteSt.executeUpdate(deleteUser);
            deleteSt.executeUpdate(deleteUserDetail);
            deleteSt.executeUpdate(deleteBrowsingHistory);
            deleteSt.executeUpdate(deleteThread);
            deleteSt.executeUpdate(deleteThreadVote);
            deleteSt.executeUpdate(deletePost);
            deleteSt.executeUpdate(deletePostVote);
            deleteSt.executeUpdate(deleteComment);
            deleteSt.executeUpdate(deleteCommentVote);
            deleteSt.executeUpdate(deleteFile);
            deleteSt.executeUpdate(deleteMessage);
            deleteSt.executeUpdate(deletePasscode);
            deleteSt.executeUpdate(deleteSession);
            deleteSt.executeUpdate(deleteForum);
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed resetDatabase()");
        }
    }
}
