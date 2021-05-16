package com.subb.service.database;

import com.subb.service.controller.enums.*;
import com.subb.service.database.exception.*;
import com.subb.service.database.model.account.ContactData;
import com.subb.service.database.model.account.UserData;
import com.subb.service.database.model.response.HistoryRecord;
import com.subb.service.database.model.response.Notification;
import com.subb.service.database.model.response.PrivateMessage;
import com.subb.service.database.model.site.CommentData;
import com.subb.service.database.model.site.ForumData;
import com.subb.service.database.model.site.PostData;
import com.subb.service.database.model.site.ThreadData;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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

    private static final String queryUserIdByEmail = "select user_id from user_main where user_email = ?";
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
                    EnumSessionStatus sessionStatus = EnumSessionStatus.fromString(rs.getString(5));
                    if (sessionStatus == EnumSessionStatus.SESSION_STATUS_VALID) {
                        if (sessionExpireTime.isBefore(Instant.now())) {
                            setSessionStatus(sessionId, EnumSessionStatus.SESSION_STATUS_EXPIRED);
                            throw new SessionExpiredException();
                        } else {
                            return sessionUserId;
                        }
                    } else if (sessionStatus == EnumSessionStatus.SESSION_STATUS_EXPIRED) {
                        throw new SessionExpiredException();
                    } else if (sessionStatus == EnumSessionStatus.SESSION_STATUS_REVOKED) {
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
            "(user_email, user_name, user_password_hash, user_privilege) " +
            "values (?, 'New User', 'password_placeholder', ?)";
    private static final String insertAccountDetail = "insert into user_detail " +
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
            insertAccountSt.setString(2, EnumUserPrivilege.PRIVILEGE_NORMAL.toString());
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
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement setSessionStatusUserAllSt = con.prepareStatement(setUserAllSessionStatus);
             PreparedStatement updateUserPasswordSt = con.prepareStatement(updateUserPassword)) {
            String hash = passwordHash(newPassword);
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

    private static final String updateUserGender = "update user_detail set user_gender = ? where user_id = ?";
    public static void modifyUserGender(int userId, EnumGender newGender) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement updateUserGenderSt = con.prepareStatement(updateUserGender)) {
            updateUserGenderSt.setString(1, newGender.toString());
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

    private static final String updateUserPersonalInfo = "update user_detail set user_personal_info = ? where user_id = ?";
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

    private static final String updateLoginTimestamp = "update user_main set user_timestamp = ? where user_id = ?";
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
            "p1.user_id, p1.user_email, p1.user_name, p1.user_password_hash, p1.user_privilege, " +
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
                            .privilege(EnumUserPrivilege.fromString(rs.getString(5)))
                            .gender(EnumGender.fromString(rs.getString(6)))
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
                            .privilege(EnumUserPrivilege.fromString(rs.getString(5)))
                            .gender(EnumGender.fromString(rs.getString(6)))
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

    private static final String queryNotification = "select notification_id, notification_user_id, notification_reply_id, notification_type, notification_status " +
            "from notification where notification_user_id = ?";
    public static List<Notification> getNotification(int userId) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryNotificationSt = con.prepareStatement(queryNotification)) {
            queryNotificationSt.setInt(1, userId);
            try (ResultSet rs = queryNotificationSt.executeQuery()) {
                List<Notification> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(Notification.builder()
                            .notificationId(rs.getInt(1))
                            .notificationUserId(rs.getInt(2))
                            .notificationReplyId(rs.getInt(3))
                            .notificationType(EnumNotificationType.fromString(rs.getString(4)))
                            .notificationStatus(EnumNotificationStatus.fromString(rs.getString(5)))
                            .build());
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getNotification(int userId)");
        }
    }

    private static final String setNotificationStatus = "update notification set notification_status = ? where notification_id = ? and notification_user_id = ?";
    private static final String setUserAllNotificationStatus = "update notification set notification_status = ? where notification_user_id = ?";
    public static void readNotification(int userId, int notificationId) {
        if (notificationId == 0) {
            try (Connection con = DriverManager.getConnection(url, user, password);
                 PreparedStatement setUserAllNotificationStatusSt = con.prepareStatement(setUserAllNotificationStatus)) {
                setUserAllNotificationStatusSt.setString(1, EnumNotificationStatus.NOTIFICATION_STATUS_READ.toString());
                setUserAllNotificationStatusSt.setInt(2, userId);
                setUserAllNotificationStatusSt.executeUpdate();
            } catch (SQLException e) {
                logger.info(e.getMessage());
                throw new DataAccessException("MySQL Execution Failed readNotification(int userId, int notificationId)");
            }
        } else {
            try (Connection con = DriverManager.getConnection(url, user, password);
                 PreparedStatement setNotificationStatusSt = con.prepareStatement(setNotificationStatus)) {
                setNotificationStatusSt.setString(1, EnumNotificationStatus.NOTIFICATION_STATUS_READ.toString());
                setNotificationStatusSt.setInt(2, notificationId);
                setNotificationStatusSt.setInt(3, userId);
                setNotificationStatusSt.executeUpdate();
            } catch (SQLException e) {
                logger.info(e.getMessage());
                throw new DataAccessException("MySQL Execution Failed readNotification(int userId, int notificationId)");
            }
        }
    }

    private static final int historyRecordsPerPage = 30;
    private static final String queryBrowsingHistory = "select record_id, record_user_id, record_thread_id, record_timestamp " +
            "from history where record_user_id = ? limit ?, ?";
    public static List<HistoryRecord> getBrowsingHistory(int userId, int page)  {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryBrowsingHistorySt = con.prepareStatement(queryBrowsingHistory)) {
            queryBrowsingHistorySt.setInt(1, userId);
            queryBrowsingHistorySt.setInt(2, historyRecordsPerPage * (page - 1));
            queryBrowsingHistorySt.setInt(3, historyRecordsPerPage);
            try (ResultSet rs = queryBrowsingHistorySt.executeQuery()) {
                List<HistoryRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(HistoryRecord.builder()
                            .historyRecordId(rs.getInt(1))
                            .historyUserId(rs.getInt(2))
                            .historyThreadId(rs.getInt(3))
                            .historyTimestamp(rs.getTimestamp(4).toInstant())
                            .build());
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getBrowsingHistory(int userId, int page)");
        }
    }

    private static final String queryThreadHistory = "select thread_id " +
            "from thread where thread_author = ? limit ?, ?";
    public static List<ThreadData> getThreadHistory(int userId, int page) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryThreadHistorySt = con.prepareStatement(queryThreadHistory)) {
            queryThreadHistorySt.setInt(1, userId);
            queryThreadHistorySt.setInt(2, historyRecordsPerPage * (page - 1));
            queryThreadHistorySt.setInt(3, historyRecordsPerPage);
            try (ResultSet rs = queryThreadHistorySt.executeQuery()) {
                List<ThreadData> result = new ArrayList<>();
                while (rs.next()) {
                    int threadId = rs.getInt(1);
                    ThreadData threadData = getThreadData(threadId);
                    result.add(threadData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getThreadHistory(int userId, int page)");
        } catch (ThreadNotExistException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get thread history");
        }
    }

    private static final String queryPostHistory = "select post_id " +
            "from post where post_author = ? limit ?, ?";
    public static List<PostData> getPostHistory(int userId, int page) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryPostHistorySt = con.prepareStatement(queryPostHistory)) {
            queryPostHistorySt.setInt(1, userId);
            queryPostHistorySt.setInt(2, historyRecordsPerPage * (page - 1));
            queryPostHistorySt.setInt(3, historyRecordsPerPage);
            try (ResultSet rs = queryPostHistorySt.executeQuery()) {
                List<PostData> result = new ArrayList<>();
                while (rs.next()) {
                    int postId = rs.getInt(1);
                    PostData postData = getPostData(postId);
                    result.add(postData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getPostHistory(int userId, int page)");
        } catch (PostNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get post history");
        }
    }

    private static final String queryCommentHistory = "select comment_id " +
            "from comment where comment_author = ? limit ?, ?";
    public static List<CommentData> getCommentHistory(int userId, int page) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryCommentHistorySt = con.prepareStatement(queryCommentHistory)) {
            queryCommentHistorySt.setInt(1, userId);
            queryCommentHistorySt.setInt(2, historyRecordsPerPage * (page - 1));
            queryCommentHistorySt.setInt(3, historyRecordsPerPage);
            try (ResultSet rs = queryCommentHistorySt.executeQuery()) {
                List<CommentData> result = new ArrayList<>();
                while (rs.next()) {
                    int commentId = rs.getInt(1);
                    CommentData commentData = getCommentData(commentId);
                    result.add(commentData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getCommentHistory(int userId, int page)");
        } catch (CommentNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get comment history");
        }
    }

    private static final String queryHomepage = "select thread_id from thread where thread_create_timestamp > ? order by thread_heat desc limit 10";
    public static List<ThreadData> getHomepage() {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryHomepageSt = con.prepareStatement(queryHomepage)) {
            queryHomepageSt.setTimestamp(1, Timestamp.from(Instant.now().minus(Duration.ofDays(1))));
            try (ResultSet rs = queryHomepageSt.executeQuery()) {
                List<ThreadData> result = new ArrayList<>();
                while (rs.next()) {
                    int threadId = rs.getInt(1);
                    ThreadData threadData = getThreadData(threadId);
                    result.add(threadData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getHomepage()");
        } catch (ThreadNotExistException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get homepage");
        }
    }

    private static final String queryForumList = "select forum_id, forum_title, forum_threads, forum_heat from forum";
    public static List<ForumData> getForumList() {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getForumListSt = con.prepareStatement(queryForumList)) {
            try (ResultSet rs = getForumListSt.executeQuery()) {
                List<ForumData> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(ForumData.builder()
                        .forumId(rs.getInt(1))
                        .title(rs.getString(2))
                        .threads(rs.getInt(3))
                        .heat(rs.getInt(4))
                        .build());
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getForumList()");
        }
    }

    private static final int threadsPerPage = 30;
    private static final String getForumPage = "select thread_id from thread where thread_forum_id = ? order by thread_active_timestamp desc limit ?, ?";
    @SuppressWarnings("unused")
    public static List<ThreadData> getForumPage(int forumId, int page) throws MalformedRequestException {
        try {
            ForumData forumData = getForumData(forumId);
        } catch (ForumNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getForumPageSt = con.prepareStatement(getForumPage)) {
            getForumPageSt.setInt(1, forumId);
            getForumPageSt.setInt(2, threadsPerPage * (page - 1));
            getForumPageSt.setInt(3, threadsPerPage);
            try (ResultSet rs = getForumPageSt.executeQuery()) {
                List<ThreadData> result = new ArrayList<>();
                while (rs.next()) {
                    int threadId = rs.getInt(1);
                    ThreadData threadData = getThreadData(threadId);
                    result.add(threadData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getForumPage(int forumId, int page)");
        } catch (ThreadNotExistException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get forum page");
        }
    }

    private static final int postsPerPage = 30;
    private static final String getThreadPage = "select post_id from post where post_thread_id = ? limit ?, ?";
    @SuppressWarnings("unused")
    public static List<PostData> getThreadPage(int threadId, int page) throws MalformedRequestException {
        try {
            ThreadData threadData = getThreadData(threadId);
        } catch (ThreadNotExistException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getThreadPageSt = con.prepareStatement(getThreadPage)) {
            getThreadPageSt.setInt(1, threadId);
            getThreadPageSt.setInt(2, postsPerPage * (page - 1));
            getThreadPageSt.setInt(3, postsPerPage);
            if (page == 0) { modifyThreadViews(threadId, 1); }
            try (ResultSet rs = getThreadPageSt.executeQuery()) {
                List<PostData> result = new ArrayList<>();
                while (rs.next()) {
                    int postId = rs.getInt(1);
                    PostData postData = getPostData(postId);
                    result.add(postData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getThreadPage(int threadId, int page)");
        } catch (PostNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get thread page");
        }
    }

    private static final int commentsPerPage = 30;
    private static final String getPostPage = "select comment_id from comment where comment_post_id = ? limit ?, ?";
    @SuppressWarnings("unused")
    public static List<CommentData> getPostPage(int postId, int page) throws MalformedRequestException {
        try {
            PostData postData = getPostData(postId);
        } catch (PostNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getPostPageSt = con.prepareStatement(getPostPage)) {
            getPostPageSt.setInt(1, postId);
            getPostPageSt.setInt(2, commentsPerPage * (page - 1));
            getPostPageSt.setInt(3, commentsPerPage);
            try (ResultSet rs = getPostPageSt.executeQuery()) {
                List<CommentData> result = new ArrayList<>();
                while (rs.next()) {
                    int commentId = rs.getInt(1);
                    CommentData commentData = getCommentData(commentId);
                    result.add(commentData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getPostPage(int postId, int page)");
        } catch (CommentNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get post page");
        }
    }

    private static final String getCommentPage = "select comment_id from comment where comment_root_id = ? limit ?, ?";
    @SuppressWarnings("unused")
    public static List<CommentData> getCommentPage(int commentId, int page) throws MalformedRequestException {
        try {
            CommentData commentData = getCommentData(commentId);
        } catch (CommentNotExistsException e) {
            throw new MalformedRequestException();
        }
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement getCommentPageSt = con.prepareStatement(getCommentPage)) {
            getCommentPageSt.setInt(1, commentId);
            getCommentPageSt.setInt(2, commentsPerPage * (page - 1));
            getCommentPageSt.setInt(3, commentsPerPage);
            try (ResultSet rs = getCommentPageSt.executeQuery()) {
                List<CommentData> result = new ArrayList<>();
                while (rs.next()) {
                    int childCommentId = rs.getInt(1);
                    CommentData childCommentData = getCommentData(childCommentId);
                    result.add(childCommentData);
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed getCommentPage(int commentId, int page)");
        } catch (CommentNotExistsException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("Server error - get comment page");
        }
    }

    private static final String queryForum = "select " +
            "forum_id, forum_title, forum_threads, forum_heat from forum where forum_id = ?";
    private static final String queryThread = "select " +
            "thread_id, thread_forum_id, thread_title, thread_author, thread_create_timestamp, thread_active_timestamp, thread_status, thread_posts, thread_views, thread_votes, thread_heat from thread where thread_id = ?";
    private static final String queryPost = "select " +
            "post_id, post_thread_id, post_author, post_timestamp, post_quote_id, post_content, post_status, post_comments, post_votes from post where post_id = ?";
    private static final String queryComment = "select " +
            "comment_id, comment_post_id, comment_root_id, comment_author, comment_timestamp, comment_quote_id, comment_content, comment_status, comment_comments, comment_votes from comment where comment_id = ?";
    public static ForumData getForumData(int forumId) throws ForumNotExistsException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryForumSt = con.prepareStatement(queryForum)) {
            queryForumSt.setInt(1, forumId);
            try (ResultSet rs = queryForumSt.executeQuery()) {
                if (rs.next()) {
                    return ForumData.builder()
                            .forumId(rs.getInt(1))
                            .title(rs.getString(2))
                            .threads(rs.getInt(3))
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
                            .status(EnumThreadStatus.fromString(rs.getString(7)))
                            .posts(rs.getInt(8))
                            .views(rs.getInt(9))
                            .votes(rs.getInt(10))
                            .heat(rs.getInt(11)).build();
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
                            .quoteId(rs.getInt(5))
                            .content(rs.getString(6))
                            .status(EnumPostStatus.fromString(rs.getString(7)))
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
                            .quoteId(rs.getInt(6))
                            .content(rs.getString(7))
                            .status(EnumCommentStatus.fromString(rs.getString(8)))
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

    private static final String insertMessage = "insert into message (message_sender, message_receiver, message_type, message_status, message_content, message_timestamp) " +
            "values (?, ?, ?, ?, ?, ?)";
    @SuppressWarnings("unused")
    public static void pushPrivateMessage(int sender, int receiver, String content, EnumMessageType messageType) throws UserNotExistsException {
        ContactData receiverData = getUserData(receiver);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertMessageSt = con.prepareStatement(insertMessage)) {
            insertMessageSt.setInt(1, sender);
            insertMessageSt.setInt(2, receiver);
            insertMessageSt.setString(3, messageType.toString());
            insertMessageSt.setString(4, EnumMessageStatus.MESSAGE_STATUS_UNREAD.toString());
            insertMessageSt.setString(5, content);
            insertMessageSt.setTimestamp(6, Timestamp.from(Instant.now()));
            insertMessageSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed pushPrivateMessage(int sender, int receiver, String content, EnumMessageType messageType)");
        }
    }

    private static final String queryUserMessageList = "select message_id, message_sender, message_receiver, message_type, message_status, message_content, message_timestamp " +
            "from message where message_sender = ? or message_receiver = ?";
    public static List<PrivateMessage> fetchPrivateMessage(int userId) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement queryUserMessageListSt = con.prepareStatement(queryUserMessageList)) {
            queryUserMessageListSt.setInt(1, userId);
            queryUserMessageListSt.setInt(2, userId);
            try (ResultSet rs = queryUserMessageListSt.executeQuery()) {
                List<PrivateMessage> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(PrivateMessage.builder()
                            .messageId(rs.getInt(1))
                            .messageSender(rs.getInt(2))
                            .messageReceiver(rs.getInt(3))
                            .messageType(EnumMessageType.fromString(rs.getString(4)))
                            .messageStatus(EnumMessageStatus.fromString(rs.getString(5)))
                            .messageContent(rs.getString(6))
                            .messageTimestamp(rs.getTimestamp(7).toInstant()).build());
                }
                return result;
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed fetchPrivateMessage(int userId)");
        }
    }

    private static final String setUserAllMessageStatus = "update message set message_status = ? where message_receiver = ?";
    private static final String setMessageStatus = "update message set message_status = ? where message_id = ? and message_receiver = ?";
    public static void readPrivateMessage(int userId, int messageId) {
        if (messageId == 0) {
            try (Connection con = DriverManager.getConnection(url, user, password);
                 PreparedStatement setUserAllMessageStatusSt = con.prepareStatement(setUserAllMessageStatus)) {
                setUserAllMessageStatusSt.setString(1, EnumMessageStatus.MESSAGE_STATUS_READ.toString());
                setUserAllMessageStatusSt.setInt(2, userId);
                setUserAllMessageStatusSt.executeUpdate();
            } catch (SQLException e) {
                logger.info(e.getMessage());
                throw new DataAccessException("MySQL Execution Failed readPrivateMessage(int userId, int messageId)");
            }
        } else {
            try (Connection con = DriverManager.getConnection(url, user, password);
                 PreparedStatement setMessageStatusSt = con.prepareStatement(setMessageStatus)) {
                setMessageStatusSt.setString(1, EnumMessageStatus.MESSAGE_STATUS_READ.toString());
                setMessageStatusSt.setInt(2, messageId);
                setMessageStatusSt.setInt(3, userId);
                setMessageStatusSt.executeUpdate();
            } catch (SQLException e) {
                logger.info(e.getMessage());
                throw new DataAccessException("MySQL Execution Failed readPrivateMessage(int userId, int messageId)");
            }
        }
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
            insertPasscodeSt.setString(5, EnumPasscodeStatus.PASSCODE_STATUS_VALID.toString());
            insertPasscodeSt.executeUpdate();
            return passcode;
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newPasscode(String email)");
        }
    }

    private static final String setPasscodeStatus = "update passcode set passcode_status = ? where passcode = ?";
    @SuppressWarnings("SameParameterValue")
    private static void setPasscodeStatus(String passcode, EnumPasscodeStatus newStatus) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement setPasscodeStatusSt = con.prepareStatement(setPasscodeStatus)) {
            setPasscodeStatusSt.setString(1, newStatus.toString());
            setPasscodeStatusSt.setString(2, passcode);
            setPasscodeStatusSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed setPasscodeStatus(String passcode, EnumPasscodeStatus newStatus)");
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
                    EnumPasscodeStatus passcodeStatus = EnumPasscodeStatus.fromString(rs.getString(4));
                    if (passcodeStatus == EnumPasscodeStatus.PASSCODE_STATUS_VALID) {
                        if (passcodeExpireTime.isBefore(Instant.now())) {
                            setPasscodeStatus(passcode, EnumPasscodeStatus.PASSCODE_STATUS_EXPIRED);
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

    private static final String setSessionStatus = "update session set session_status = ? where session_id = ?";
    @SuppressWarnings("SameParameterValue")
    private static void setSessionStatus(String session, EnumSessionStatus newStatus) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement setSessionStatusSt = con.prepareStatement(setSessionStatus)) {
            setSessionStatusSt.setString(1, newStatus.toString());
            setSessionStatusSt.setString(2, session);
            setSessionStatusSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed setSessionStatus(String session, EnumSessionStatus newStatus)");
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

    public static void revokeSession(String session) {
        setSessionStatus(session, EnumSessionStatus.SESSION_STATUS_REVOKED);
    }

    private static void notifyThreadAuthor(int threadId, int replyId) throws ThreadNotExistException {
        ThreadData threadData = getThreadData(threadId);
        notify(threadData.getAuthor(), replyId, EnumNotificationType.ENUM_NOTIFICATION_TYPE_THREAD_AUTHOR);
    }

    private static void notifyPostAuthor(int postId, int replyId) throws ThreadNotExistException, PostNotExistsException {
        PostData postData = getPostData(postId);
        notify(postData.getAuthor(), replyId, EnumNotificationType.ENUM_NOTIFICATION_TYPE_POST_AUTHOR);
        notifyThreadAuthor(postData.getThreadId(), replyId);
    }

    private static void notifyCommentRootAuthor(int commentId, int replyId) throws ThreadNotExistException, PostNotExistsException, CommentNotExistsException {
        CommentData commentData = getCommentData(commentId);
        notify(commentData.getAuthor(), replyId, EnumNotificationType.ENUM_NOTIFICATION_TYPE_COMMENT_ROOT_AUTHOR);
        notifyPostAuthor(commentData.getPostId(), replyId);
    }

    private static final String insertNotification = "insert into notification " +
            "(notification_user_id, notification_reply_id, notification_type, notification_status) " +
            "values (?, ?, ?, ?) " +
            "on duplicate key update notification_id = notification_id";
    private static void notify(int userId, int replyId, EnumNotificationType notificationType) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertNotificationSt = con.prepareStatement(insertNotification)) {
            insertNotificationSt.setInt(1, userId);
            insertNotificationSt.setInt(2, replyId);
            insertNotificationSt.setString(3, notificationType.toString());
            insertNotificationSt.setString(4, EnumNotificationStatus.NOTIFICATION_STATUS_UNREAD.toString());
            insertNotificationSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed notify(int userId, int replyId, EnumNotificationType notificationType)");
        }
    }

    private static final String insertThread = "insert into thread " +
            "(thread_forum_id, thread_title, thread_author, thread_create_timestamp, thread_active_timestamp, thread_status, thread_posts, thread_views, thread_votes, thread_heat) " +
            "values (?, ?, ?, ?, ?, ?, 0, 0, 0, 0)";
    @SuppressWarnings("unused")
    public static int newThread(int authorId, int forumId, String threadTitle) throws MalformedRequestException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertThreadSt = con.prepareStatement(insertThread);
             PreparedStatement queryLastInsertedSt = con.prepareStatement(queryLastInserted)) {
            ForumData forumData = getForumData(forumId);
            insertThreadSt.setInt(1, forumId);
            insertThreadSt.setString(2, threadTitle);
            insertThreadSt.setInt(3, authorId);
            insertThreadSt.setTimestamp(4, Timestamp.from(Instant.now()));
            insertThreadSt.setTimestamp(5, Timestamp.from(Instant.now()));
            insertThreadSt.setString(6, EnumThreadStatus.THREAD_STATUS_VISIBLE.toString());
            insertThreadSt.executeUpdate();
            modifyForumThreads(forumId, 1);
            try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DataAccessException("Unexpected Error - Create New Thread!");
                }
            }
        } catch (ForumNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newThread(int authorId, int forumId, String threadTitle, String threadContent)");
        }
    }

    private static final String insertPost = "insert into post " +
            "(post_thread_id, post_author, post_timestamp, post_quote_id, post_content, post_status, post_comments, post_votes) " +
            "values (?, ?, ?, ?, ?, ?, 0, 0)";
    @SuppressWarnings("unused")
    public static void newPost(int authorId, int threadId, int quoteId, String postContent) throws MalformedRequestException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertPostSt = con.prepareStatement(insertPost);
             PreparedStatement queryLastInsertedSt = con.prepareStatement(queryLastInserted)) {
            ThreadData threadData = getThreadData(threadId);
            if (quoteId == 0) {
                insertPostSt.setInt(1, threadId);
                insertPostSt.setInt(2, authorId);
                insertPostSt.setTimestamp(3, Timestamp.from(Instant.now()));
                insertPostSt.setInt(4, quoteId);
                insertPostSt.setString(5, postContent);
                insertPostSt.setString(6, EnumPostStatus.POST_STATUS_VISIBLE.toString());
                insertPostSt.executeUpdate();
                modifyThreadPosts(threadId, 1);
                setThreadActiveTimestamp(threadId);
                try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                    if (rs.next()) {
                        int newPostId = rs.getInt(1);
                        notifyThreadAuthor(threadId, newPostId);
                    }
                } catch (ThreadNotExistException e) {
                    logger.info(e.getMessage());
                    throw new DataAccessException("Server error - new post");
                }
            } else {
                PostData quotedPostData = getPostData(quoteId);
                if (quotedPostData.getThreadId() != threadId) {
                    throw new MalformedRequestException();
                }
                insertPostSt.setInt(1, threadId);
                insertPostSt.setInt(2, authorId);
                insertPostSt.setTimestamp(3, Timestamp.from(Instant.now()));
                insertPostSt.setInt(4, quoteId);
                insertPostSt.setString(5, postContent);
                insertPostSt.setString(6, EnumPostStatus.POST_STATUS_VISIBLE.toString());
                insertPostSt.executeUpdate();
                modifyThreadPosts(threadId, 1);
                try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                    if (rs.next()) {
                        int newPostId = rs.getInt(1);
                        notify(quotedPostData.getAuthor(), newPostId, EnumNotificationType.ENUM_NOTIFICATION_TYPE_POST_QUOTE);
                        notifyThreadAuthor(threadId, newPostId);
                    }
                } catch (ThreadNotExistException e) {
                    logger.info(e.getMessage());
                    throw new DataAccessException("Server error - new post");
                }
            }
        } catch (ThreadNotExistException | PostNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newPost(int authorId, int threadId, int quoteId, String postContent)");
        }
    }

    private static final String insertComment = "insert into comment " +
            "(comment_post_id, comment_root_id, comment_author, comment_timestamp, comment_quote_id, comment_content, comment_status, comment_comments, comment_votes) " +
            "values (?, ?, ?, ?, ?, ?, ?, 0, 0)";
    public static void newComment(int authorId, int postId, int quoteId, String commentContent) throws MalformedRequestException {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement insertCommentSt = con.prepareStatement(insertComment);
             PreparedStatement queryLastInsertedSt = con.prepareStatement(queryLastInserted)) {
            PostData postData = getPostData(postId);
            if (quoteId == 0) {
                insertCommentSt.setInt(1, postId);
                insertCommentSt.setInt(2, 0);
                insertCommentSt.setInt(3, authorId);
                insertCommentSt.setTimestamp(4, Timestamp.from(Instant.now()));
                insertCommentSt.setInt(5, quoteId);
                insertCommentSt.setString(6, commentContent);
                insertCommentSt.setString(7, EnumCommentStatus.COMMENT_STATUS_VISIBLE.toString());
                insertCommentSt.executeUpdate();
                modifyPostComments(postId, 1);
                setThreadActiveTimestamp(postData.getThreadId());
                try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                    if (rs.next()) {
                        notifyPostAuthor(postId, rs.getInt(1));
                    }
                } catch (ThreadNotExistException | PostNotExistsException e) {
                    logger.info(e.getMessage());
                    throw new DataAccessException("Server error - new comment");
                }
            } else {
                CommentData quotedCommentData = getCommentData(quoteId);
                if (quotedCommentData.getPostId() != postId) {
                    throw new MalformedRequestException();
                }
                if (quotedCommentData.getRootId() == 0) {
                    insertCommentSt.setInt(1, postId);
                    insertCommentSt.setInt(2, quotedCommentData.getCommentId());
                    insertCommentSt.setInt(3, authorId);
                    insertCommentSt.setTimestamp(4, Timestamp.from(Instant.now()));
                    insertCommentSt.setInt(5, quoteId);
                    insertCommentSt.setString(6, commentContent);
                    insertCommentSt.setString(7, EnumCommentStatus.COMMENT_STATUS_VISIBLE.toString());
                    insertCommentSt.executeUpdate();
                    modifyPostComments(postId, 1);
                    modifyCommentComments(quoteId, 1);
                    setThreadActiveTimestamp(postData.getThreadId());
                    try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                        if (rs.next()) {
                            int newCommentId = rs.getInt(1);
                            notifyCommentRootAuthor(quotedCommentData.getCommentId(), newCommentId);
                        }
                    } catch (ThreadNotExistException | PostNotExistsException e) {
                        logger.info(e.getMessage());
                        throw new DataAccessException("Server error - new comment");
                    }
                } else {
                    CommentData rootCommentData = getCommentData(quotedCommentData.getRootId());
                    insertCommentSt.setInt(1, postId);
                    insertCommentSt.setInt(2, rootCommentData.getCommentId());
                    insertCommentSt.setInt(3, authorId);
                    insertCommentSt.setTimestamp(4, Timestamp.from(Instant.now()));
                    insertCommentSt.setInt(5, quoteId);
                    insertCommentSt.setString(6, commentContent);
                    insertCommentSt.setString(7, EnumCommentStatus.COMMENT_STATUS_VISIBLE.toString());
                    insertCommentSt.executeUpdate();
                    modifyPostComments(postId, 1);
                    modifyCommentComments(rootCommentData.getCommentId(), 1);
                    setThreadActiveTimestamp(postData.getThreadId());
                    try (ResultSet rs = queryLastInsertedSt.executeQuery()) {
                        if (rs.next()) {
                            int newCommentId = rs.getInt(1);
                            notify(quotedCommentData.getAuthor(), newCommentId, EnumNotificationType.ENUM_NOTIFICATION_TYPE_COMMENT_QUOTE);
                            notifyCommentRootAuthor(rootCommentData.getCommentId(), newCommentId);
                        }
                    } catch (ThreadNotExistException | PostNotExistsException | CommentNotExistsException e) {
                        logger.info(e.getMessage());
                        throw new DataAccessException("Server error - new comment");
                    }
                }
            }
        } catch (PostNotExistsException | CommentNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed newComment(int authorId, int postId, int quoteId, String commentContent)");
        }
    }

    private static final String setThreadActiveTimestamp = "update thread set thread_active_timestamp = ? where thread_id = ?";
    private static void setThreadActiveTimestamp(int threadId) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement setThreadActiveTimestampSt = con.prepareStatement(setThreadActiveTimestamp)) {
            setThreadActiveTimestampSt.setTimestamp(1, Timestamp.from(Instant.now()));
            setThreadActiveTimestampSt.setInt(2, threadId);
            setThreadActiveTimestampSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed setThreadActiveTimestamp(int threadId)");
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
            deleteThreadSt.setString(1, EnumThreadStatus.THREAD_STATUS_DELETED.toString());
            deleteThreadSt.setInt(2, threadId);
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
            deletePostSt.setString(1, EnumPostStatus.POST_STATUS_DELETED.toString());
            deletePostSt.setInt(2, postId);
            deletePostSt.executeUpdate();
        } catch (ThreadNotExistException | PostNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed deletePost(int userId, int postId)");
        }
    }

    private static final String setCommentStatus = "update comment set comment_status = ? where comment_id = ?";
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
            deleteCommentSt.setString(1, EnumCommentStatus.COMMENT_STATUS_DELETED.toString());
            deleteCommentSt.setInt(2, commentId);
            deleteCommentSt.executeUpdate();
        } catch (ThreadNotExistException | PostNotExistsException | CommentNotExistsException e) {
            throw new MalformedRequestException();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed deleteComment(int userId, int commentId)");
        }
    }

    private static final String queryThreadVoted = "select record_id from thread_vote where record_thread_id = ? and record_voter_id = ?";
    @SuppressWarnings("unused")
    private static boolean hasVotedThread(int voterId, int threadId) throws ThreadNotExistException {
        ThreadData threadData = getThreadData(threadId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement checkThreadVotedSt = con.prepareStatement(queryThreadVoted)) {
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

    private static final String queryPostVoted = "select record_id from post_vote where record_post_id = ? and record_voter_id = ?";
    @SuppressWarnings("unused")
    private static boolean hasVotedPost(int voterId, int postId) throws PostNotExistsException {
        PostData postData = getPostData(postId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement checkPostVotedSt = con.prepareStatement(queryPostVoted)) {
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

    private static final String queryCommentVoted = "select record_id from comment_vote where record_comment_id = ? and record_voter_id = ?";
    @SuppressWarnings("unused")
    private static boolean hasVotedComment(int voterId, int commentId) throws CommentNotExistsException {
        CommentData commentData = getCommentData(commentId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement checkCommentVotedSt = con.prepareStatement(queryCommentVoted)) {
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
    @SuppressWarnings("unused")
    public static void voteThread(int voterId, int threadId) throws ThreadNotExistException {
        ThreadData threadData = getThreadData(threadId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement voteThreadSt = con.prepareStatement(voteThread);
             PreparedStatement deleteVoteRecordSt = con.prepareStatement(deleteVoteRecordThread)) {
            if (hasVotedThread(voterId, threadId)) {
                deleteVoteRecordSt.setInt(1, threadId);
                deleteVoteRecordSt.setInt(2, voterId);
                deleteVoteRecordSt.executeUpdate();
                modifyThreadVotes(threadId, -1);
            } else {
                voteThreadSt.setInt(1, threadId);
                voteThreadSt.setInt(2, voterId);
                voteThreadSt.executeUpdate();
                modifyThreadVotes(threadId, 1);
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed voteThread(int voterId, int threadId)");
        }
    }

    private static final String votePost = "insert into post_vote " +
            "(record_post_id, record_voter_id) " +
            "values (?, ?)";
    private static final String deleteVoteRecordPost = "delete from post_vote where record_post_id = ? and record_voter_id = ?";
    @SuppressWarnings("unused")
    public static void votePost(int voterId, int postId) throws PostNotExistsException {
        PostData postData = getPostData(postId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement votePostSt = con.prepareStatement(votePost);
             PreparedStatement deleteVoteRecordSt = con.prepareStatement(deleteVoteRecordPost)) {
            if (hasVotedPost(voterId, postId)) {
                deleteVoteRecordSt.setInt(1, postId);
                deleteVoteRecordSt.setInt(2, voterId);
                deleteVoteRecordSt.executeUpdate();
                modifyPostVotes(postId, -1);
            } else {
                votePostSt.setInt(1, postId);
                votePostSt.setInt(2, voterId);
                votePostSt.executeUpdate();
                modifyPostVotes(postId, 1);
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed votePost(int voterId, int postId)");
        }
    }

    private static final String voteComment = "insert into comment_vote " +
            "(record_comment_id, record_voter_id) " +
            "values (?, ?)";
    private static final String deleteVoteRecordComment = "delete from comment_vote where record_comment_id = ? and record_voter_id = ?";
    @SuppressWarnings("unused")
    public static void voteComment(int voterId, int commentId) throws CommentNotExistsException {
        CommentData commentData = getCommentData(commentId);
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement voteCommentSt = con.prepareStatement(voteComment);
             PreparedStatement deleteVoteRecordSt = con.prepareStatement(deleteVoteRecordComment)) {
            if (hasVotedComment(voterId, commentId)) {
                deleteVoteRecordSt.setInt(1, commentId);
                deleteVoteRecordSt.setInt(2, voterId);
                deleteVoteRecordSt.executeUpdate();
                modifyCommentVotes(commentId, -1);
            } else {
                voteCommentSt.setInt(1, commentId);
                voteCommentSt.setInt(2, voterId);
                voteCommentSt.executeUpdate();
                modifyCommentVotes(commentId, 1);
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed voteComment(int voterId, int commentId)");
        }
    }

    private static final int heatPerThread = 100;
    private static final int heatPerPost = 30;
    private static final int heatPerComment = 10;
    private static final int heatPerVote = 3;
    private static final int heatPerView = 1;
    private static final String modifyForumThreads = "update forum set forum_threads = forum_threads + ? where forum_id = ?";
    private static final String modifyForumHeat = "update forum set forum_heat = forum_heat + ? where forum_id = ?";
    private static final String modifyThreadPosts = "update thread set thread_posts = thread_posts + ? where thread_id = ?";
    private static final String modifyThreadViews = "update thread set thread_views = thread_views + ? where thread_id = ?";
    private static final String modifyThreadVotes = "update thread set thread_votes = thread_votes + ? where thread_id = ?";
    private static final String modifyThreadHeat = "update thread set thread_heat = thread_heat + ? where thread_id = ?";
    private static final String modifyPostComments = "update post set post_comments = post_comments + ? where post_id = ?";
    private static final String modifyPostVotes = "update post set post_votes = post_votes + ? where post_id = ?";
    private static final String modifyCommentComments = "update comment set comment_comments = comment_comments + ? where comment_id = ?";
    private static final String modifyCommentVotes = "update comment set comment_votes = comment_votes + ? where comment_id = ?";

    @SuppressWarnings("SameParameterValue")
    private static void modifyForumThreads(int forumId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyForumThreadsSt = con.prepareStatement(modifyForumThreads)) {
            modifyForumThreadsSt.setInt(1, modifier);
            modifyForumThreadsSt.setInt(2, forumId);
            modifyForumThreadsSt.executeUpdate();
            if (modifier > 0) {
                modifyForumHeat(forumId, modifier * heatPerThread);
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyForumThreads(int forumId, int modifier)");
        }
    }

    private static void modifyForumHeat(int forumId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyForumHeatSt = con.prepareStatement(modifyForumHeat)) {
            modifyForumHeatSt.setInt(1, modifier);
            modifyForumHeatSt.setInt(2, forumId);
            modifyForumHeatSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyForumHeat(int forumId, int modifier)");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void modifyThreadPosts(int threadId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyThreadPostsSt = con.prepareStatement(modifyThreadPosts)) {
            modifyThreadPostsSt.setInt(1, modifier);
            modifyThreadPostsSt.setInt(2, threadId);
            modifyThreadPostsSt.executeUpdate();
            if (modifier > 0) {
                modifyThreadHeat(threadId, modifier * heatPerPost);
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyThreadPosts(int threadId, int modifier)");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void modifyThreadViews(int threadId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyThreadViewsSt = con.prepareStatement(modifyThreadViews)) {
            modifyThreadViewsSt.setInt(1, modifier);
            modifyThreadViewsSt.setInt(2, threadId);
            modifyThreadViewsSt.executeUpdate();
            if (modifier > 0) {
                modifyThreadHeat(threadId, modifier * heatPerView);
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyThreadViews(int threadId, int modifier)");
        }
    }

    private static void modifyThreadVotes(int threadId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyThreadVotesSt = con.prepareStatement(modifyThreadVotes)) {
            modifyThreadVotesSt.setInt(1, modifier);
            modifyThreadVotesSt.setInt(2, threadId);
            modifyThreadVotesSt.executeUpdate();
            modifyThreadHeat(threadId, modifier * heatPerVote);
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyThreadVotes(int threadId, int modifier)");
        }
    }

    private static void modifyThreadHeat(int threadId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyThreadHeatSt = con.prepareStatement(modifyThreadHeat)) {
            modifyThreadHeatSt.setInt(1, modifier);
            modifyThreadHeatSt.setInt(2, threadId);
            modifyThreadHeatSt.executeUpdate();
            if (modifier > 0) {
                try {
                    ThreadData threadData = getThreadData(threadId);
                    modifyForumHeat(threadData.getForumId(), modifier);
                } catch (ThreadNotExistException e) {
                    logger.info(e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyThreadHeat(int threadId, int modifier)");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void modifyPostComments(int postId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyPostCommentsSt = con.prepareStatement(modifyPostComments)) {
            modifyPostCommentsSt.setInt(1, modifier);
            modifyPostCommentsSt.setInt(2, postId);
            modifyPostCommentsSt.executeUpdate();
            if (modifier > 0) {
                try {
                    PostData postData = getPostData(postId);
                    modifyThreadHeat(postData.getThreadId(), modifier * heatPerComment);
                } catch (PostNotExistsException e) {
                    logger.info(e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyPostComments(int postId, int modifier)");
        }
    }

    private static void modifyPostVotes(int postId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyPostVotesSt = con.prepareStatement(modifyPostVotes)) {
            modifyPostVotesSt.setInt(1, modifier);
            modifyPostVotesSt.setInt(2, postId);
            modifyPostVotesSt.executeUpdate();
            try {
                PostData postData = getPostData(postId);
                modifyThreadHeat(postData.getThreadId(), modifier * heatPerVote);
            } catch (PostNotExistsException e) {
                logger.info(e.getMessage());
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyPostVotes(int postId, int modifier)");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void modifyCommentComments(int commentId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyCommentCommentsSt = con.prepareStatement(modifyCommentComments)) {
            modifyCommentCommentsSt.setInt(1, modifier);
            modifyCommentCommentsSt.setInt(2, commentId);
            modifyCommentCommentsSt.executeUpdate();
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyCommentComments(int commentId, int modifier)");
        }
    }

    private static void modifyCommentVotes(int commentId, int modifier) {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement modifyCommentVotesSt = con.prepareStatement(modifyCommentVotes)) {
            modifyCommentVotesSt.setInt(1, modifier);
            modifyCommentVotesSt.setInt(2, commentId);
            modifyCommentVotesSt.executeUpdate();
            try {
                CommentData commentData = getCommentData(commentId);
                PostData postData = getPostData(commentData.getPostId());
                modifyThreadHeat(postData.getThreadId(), modifier * heatPerVote);
            } catch (PostNotExistsException | CommentNotExistsException e) {
                logger.info(e.getMessage());
            }
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed modifyCommentVotes(int commentId, int modifier)");
        }
    }

    private static void validateAdministrator(int userId) throws UserNotExistsException, OperationForbiddenException {
        if(!getUserData(userId).getPrivilege().equals(EnumUserPrivilege.PRIVILEGE_ADMIN)) {
            throw new OperationForbiddenException();
        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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
    private static final String deleteHistory = "delete from history";
    private static final String deleteThread = "delete from thread";
    private static final String deleteThreadVote = "delete from thread_vote";
    private static final String deletePost = "delete from post";
    private static final String deletePostVote = "delete from post_vote";
    private static final String deleteComment = "delete from comment";
    private static final String deleteCommentVote = "delete from comment_vote";
    private static final String deleteMessage = "delete from message";
    private static final String deletePasscode = "delete from passcode";
    private static final String deleteSession = "delete from session";
    private static final String deleteForum = "delete from forum";
    private static final String deleteNotification = "delete from notification";
    public static void reset() {
        try (Connection con = DriverManager.getConnection(url, user, password);
            Statement deleteSt = con.createStatement()) {
            deleteSt.executeUpdate(deleteUser);
            deleteSt.executeUpdate(deleteUserDetail);
            deleteSt.executeUpdate(deleteHistory);
            deleteSt.executeUpdate(deleteThread);
            deleteSt.executeUpdate(deleteThreadVote);
            deleteSt.executeUpdate(deletePost);
            deleteSt.executeUpdate(deletePostVote);
            deleteSt.executeUpdate(deleteComment);
            deleteSt.executeUpdate(deleteCommentVote);
            deleteSt.executeUpdate(deleteMessage);
            deleteSt.executeUpdate(deletePasscode);
            deleteSt.executeUpdate(deleteSession);
            deleteSt.executeUpdate(deleteForum);
            deleteSt.executeUpdate(deleteNotification);
        } catch (SQLException e) {
            logger.info(e.getMessage());
            throw new DataAccessException("MySQL Execution Failed resetDatabase()");
        }
    }
}
