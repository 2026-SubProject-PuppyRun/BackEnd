package org.zerock.puppyrun.member.service;

/** 회원 탈퇴 DB 커밋 후 해제할 FCM 토큰입니다. */
public record AccountDeletionNotificationCleanupEvent(String fcmToken) {
}
