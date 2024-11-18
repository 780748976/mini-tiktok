package com.sky.pojo.constant;


public class WebRedisConstants {
    // 用户今日签到key
    public static final String USER_TODAY_SIGN_IN_KEY = "TodaySignIn:";
    // 用户登录黑名单key
    public static final String USER_LOGIN_BLACKLIST_KEY = "UserLoginBlackList:";
    // 文件上传记录key
    public static final String FILE_UPLOAD_RECORD_KEY = "FileUploadRecord:";
    public static final String EMAIL_CODE_KEY = "EmailCodeKey:";
    public static final String EMAIL_CODE_LOCK = "EmailCodeLock:";
    public static final String VIEW_VIDEO_KEY = "ViewVideo:";
    public static final String ADMIN_INVITE_CODE_KEY = "AdminInviteCode:";
    public static final String ADMIN_LOGIN_BLACKLIST_KEY = "AdminLoginBlackList:";
    public static final String USER_DYNAMICS = "UserDynamics:";
    //用户发布视频到follower时的游标
    public static final String VIDEO_PUBLISH_CURSOR = "VideoPublishCursor:";
    //需要同步到Es的视频名单记录
    public static final String VIDEO_APPEND_LIST = "VideoAppendList:";
    //system通知 comment评论通知 like点赞通知 mention@通知 message消息 dynamic动态通知
    public static final String SYSTEM_MESSAGE = "SystemMessage:";
    public static final String COMMENT_MESSAGE = "CommentMessage:";
    public static final String LIKE_MESSAGE = "LikeMessage:";
    public static final String MENTION_MESSAGE = "MentionMessage:";
    public static final String MESSAGE_MESSAGE = "MessageMessage:";
    public static final String DYNAMIC_MESSAGE = "DynamicMessage:";
}
