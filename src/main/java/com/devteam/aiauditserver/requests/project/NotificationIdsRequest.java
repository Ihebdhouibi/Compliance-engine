package com.devteam.aiauditserver.requests.project;

import java.util.List;

/** Batch payload for mark-as-read / delete notification operations. */
public class NotificationIdsRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
