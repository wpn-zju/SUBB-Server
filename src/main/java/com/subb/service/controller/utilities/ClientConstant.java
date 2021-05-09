package com.subb.service.controller.utilities;

public final class ClientConstant {
    public static final String API_SIGN_UP = "/small_talk_api/sign_up";
    public static final String API_SIGN_IN = "/small_talk_api/sign_in";
    public static final String API_SIGN_OUT = "/small_talk_api/sign_out";
    public static final String API_RECOVER_PASSWORD = "/small_talk_api/recover_password";
    public static final String API_REQUEST_PASSCODE = "/small_talk_api/request_passcode";
    public static final String API_MODIFY_INFO = "/small_talk_api/modify_info";
    public static final String API_NEW_THREAD = "/small_talk_api/new_thread";
    public static final String API_NEW_POST = "/small_talk_api/new_post";
    public static final String API_NEW_COMMENT = "/small_talk_api/new_comment";
    public static final String API_DELETE_THREAD = "/small_talk_api/delete_thread";
    public static final String API_DELETE_POST = "/small_talk_api/delete_post";
    public static final String API_DELETE_COMMENT = "/small_talk_api/delete_comment";
    public static final String API_VOTE_THREAD = "/small_talk_api/vote_thread";
    public static final String API_VOTE_POST = "/small_talk_api/vote_post";
    public static final String API_VOTE_COMMENT = "/small_talk_api/vote_comment";
    public static final String API_GET_FORUM = "/small_talk_api/get_forum";
    public static final String API_GET_THREAD = "/small_talk_api/get_thread";
    public static final String API_GET_POST = "/small_talk_api/get_post";
    public static final String API_GET_COMMENT = "/small_talk_api/get_comment";
    public static final String API_GET_HOMEPAGE = "/small_talk_api/get_homepage";
    public static final String API_GET_FORUM_PAGE = "/small_talk_api/get_forum_page";
    public static final String API_GET_THREAD_PAGE = "/small_talk_api/get_thread_page";
    public static final String API_GET_POST_PAGE = "/small_talk_api/get_post_page";
    public static final String API_GET_COMMENT_PAGE = "/small_talk_api/get_comment_page";
    public static final String API_GET_THREAD_HISTORY = "/small_talk_api/get_thread_history";
    public static final String API_GET_POST_HISTORY = "/small_talk_api/get_post_history";
    public static final String API_GET_COMMENT_HISTORY = "/small_talk_api/get_comment_history";
    public static final String API_GET_BROWSING_HISTORY = "/small_talk_api/get_browsing_history";
    public static final String API_LOAD_SELF = "/small_talk_api/load_self";
    public static final String API_LOAD_USER = "/small_talk_api/load_user";
    public static final String API_ARCHIVE_FILE = "/small_talk_api/archive_file";
    public static final String API_PUSH_PRIVATE_MESSAGE = "/small_talk_api/push_private_message";
    public static final String API_FETCH_PRIVATE_MESSAGE = "/small_talk_api/fetch_private_message";
    public static final String API_READ_PRIVATE_MESSAGE = "/small_talk_api/read_private_message";
    public static final String API_ADMIN_DISABLE_ACCOUNT = "/small_talk_api/admin_disable_account";
    public static final String API_ADMIN_DELETE_THREAD = "/small_talk_api/admin_delete_thread";
    public static final String API_ADMIN_DELETE_POST = "/small_talk_api/admin_delete_post";
    public static final String API_ADMIN_DELETE_COMMENT = "/small_talk_api/admin_delete_comment";
    public static final String API_GET_NOTIFICATION = "/small_talk_api/get_notification";
    public static final String API_READ_NOTIFICATION = "/small_talk_api/read_notification";

    public static final String SIGN_UP_EMAIL = "email";
    public static final String SIGN_UP_PASSWORD = "password";
    public static final String SIGN_UP_PASSCODE = "passcode";
    public static final String SIGN_IN_EMAIL = "email";
    public static final String SIGN_IN_PASSWORD = "password";
    public static final String RECOVER_PASSWORD_EMAIL = "email";
    public static final String RECOVER_PASSWORD_PASSWORD = "password";
    public static final String RECOVER_PASSWORD_PASSCODE = "passcode";
    public static final String REQUEST_PASSCODE_EMAIL = "email";
    public static final String MODIFY_INFO_NICKNAME = "nickname";
    public static final String MODIFY_INFO_PASSWORD = "password";
    public static final String MODIFY_INFO_GENDER = "gender";
    public static final String MODIFY_INFO_AVATAR_LINK = "avatar_link";
    public static final String MODIFY_INFO_PERSONAL_INFO = "personal_info";
    public static final String NEW_THREAD_FORUM_ID = "forum_id";
    public static final String NEW_THREAD_TITLE = "title";
    public static final String NEW_POST_THREAD_ID = "thread_id";
    public static final String NEW_POST_QUOTE_ID = "quote_id";
    public static final String NEW_COMMENT_POST_ID = "post_id";
    public static final String NEW_COMMENT_QUOTE_ID = "quote_id";
    public static final String DELETE_THREAD_THREAD_ID = "thread_id";
    public static final String DELETE_POST_POST_ID = "post_id";
    public static final String DELETE_COMMENT_COMMENT_ID = "comment_id";
    public static final String VOTE_THREAD_THREAD_ID = "thread_id";
    public static final String VOTE_POST_POST_ID = "post_id";
    public static final String VOTE_COMMENT_COMMENT_ID = "comment_id";
    public static final String GET_FORUM_FORUM_ID = "forum_id";
    public static final String GET_THREAD_THREAD_ID = "thread_id";
    public static final String GET_POST_POST_ID = "post_id";
    public static final String GET_COMMENT_COMMENT_ID = "comment_id";
    public static final String GET_FORUM_PAGE_FORUM_ID = "forum_id";
    public static final String GET_FORUM_PAGE_PAGE = "page";
    public static final String GET_THREAD_PAGE_THREAD_ID = "thread_id";
    public static final String GET_THREAD_PAGE_PAGE = "page";
    public static final String GET_POST_PAGE_POST_ID = "post_id";
    public static final String GET_POST_PAGE_PAGE = "page";
    public static final String GET_COMMENT_PAGE_COMMENT_ID = "comment_id";
    public static final String GET_COMMENT_PAGE_PAGE = "page";
    public static final String GET_THREAD_HISTORY_PAGE = "page";
    public static final String GET_POST_HISTORY_PAGE = "page";
    public static final String GET_COMMENT_HISTORY_PAGE = "page";
    public static final String GET_BROWSING_HISTORY_PAGE = "page";
    public static final String LOAD_USER_USER_ID = "user_id";
    public static final String ARCHIVE_FILE_NAME = "name";
    public static final String ARCHIVE_FILE_LINK = "link";
    public static final String ARCHIVE_FILE_UPLOADER = "uploader";
    public static final String ARCHIVE_FILE_SIZE = "size";
    public static final String PUSH_PRIVATE_MESSAGE_RECEIVER = "receiver";
    public static final String PUSH_PRIVATE_MESSAGE_CONTENT = "content";
    public static final String READ_PRIVATE_MESSAGE_MESSAGE_ID = "message_id";
    public static final String ADMIN_DISABLE_ACCOUNT_USER_ID = "user_id";
    public static final String ADMIN_DISABLE_ACCOUNT_PERIOD = "period";
    public static final String ADMIN_DELETE_THREAD_THREAD_ID = "thread_id";
    public static final String ADMIN_DELETE_POST_POST_ID = "post_id";
    public static final String ADMIN_DELETE_COMMENT_COMMENT_ID = "comment_id";
    public static final String READ_NOTIFICATION_NOTIFICATION_ID = "notification_id";
}
