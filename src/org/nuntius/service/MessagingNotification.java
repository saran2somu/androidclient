package org.nuntius.service;

import org.nuntius.R;
import org.nuntius.data.Contact;
import org.nuntius.provider.MyMessages.Threads;
import org.nuntius.ui.ComposeMessage;
import org.nuntius.ui.ConversationList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;


/**
 * Various utility methods for managing system notifications.
 * @author Daniele Ricci
 * @version 1.0
 */
public abstract class MessagingNotification {
    private static final int MESSAGES_NOTIFICATION_ID = 12;

    private static final String[] THREADS_UNREAD_PROJECTION =
    {
        Threads._ID,
        Threads.PEER,
        Threads.CONTENT,
        Threads.UNREAD,
        Threads.TIMESTAMP
    };

    private static final String THREADS_UNREAD_SELECTION =
        Threads.UNREAD + " > 0";

    /**
     * Updates system notification for unread messages.
     * @param context
     * @param isNew if true a new message has come (starts notification alerts)
     */
    public static void updateMessagesNotification(Context context, boolean isNew) {
        ContentResolver res = context.getContentResolver();
        NotificationManager nm = (NotificationManager) context
            .getSystemService(Context.NOTIFICATION_SERVICE);

        // query for unread threads
        Cursor c = res.query(Threads.CONTENT_URI,
                THREADS_UNREAD_PROJECTION, THREADS_UNREAD_SELECTION, null,
                Threads.INVERTED_SORT_ORDER);

        // no unread messages - delete notification
        if (c.getCount() == 0) {
            c.close();
            nm.cancel(MESSAGES_NOTIFICATION_ID);
            return;
        }

        // loop all threads and accumulate them
        MessageAccumulator accumulator = new MessageAccumulator(context);
        while (c.moveToNext()) {
            accumulator.accumulate(
                c.getLong(0),
                c.getString(1),
                c.getString(2),
                c.getInt(3),
                c.getLong(4)
            );
        }
        c.close();

        Notification no = new Notification(R.drawable.icon, accumulator.getTicker(), accumulator.getTimestamp());
        no.defaults |= Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
        no.flags |= Notification.FLAG_SHOW_LIGHTS;

        no.setLatestEventInfo(context.getApplicationContext(),
                accumulator.getTitle(), accumulator.getText(), accumulator.getPendingIntent());
        nm.notify(MESSAGES_NOTIFICATION_ID, no);
    }

    private static final class MessageAccumulator {
        private final class ConversationStub {
            public long id;
            public String peer;
            public String content;
            public long timestamp;
        }

        private ConversationStub conversation;
        private int convCount;
        private int unreadCount;
        private Context mContext;

        public MessageAccumulator(Context context) {
            mContext = context;
        }

        public void accumulate(long id, String peer, String content, int unread, long timestamp) {
            // check old accumulated conversation
            if (conversation != null) {
                if (!conversation.peer.equalsIgnoreCase(peer))
                    convCount++;
            }
            // no previous conversation - start counting
            else {
                convCount = 1;
                conversation = new ConversationStub();
            }

            conversation.id = id;
            conversation.peer = peer;
            conversation.content = content;
            conversation.timestamp = timestamp;

            unreadCount += unread;
        }

        /** Returns the text that should be used as a ticker in the notification. */
        public String getTicker() {
            return conversation.content;
        }

        /** Returns the text that should be used as the notification title. */
        public String getTitle() {
            if (convCount > 1) {
                return mContext.getString(R.string.new_messages);
            }
            else {
                // FIXME use a contact cache
                Contact contact = Contact.findbyUserId(mContext, conversation.peer);
                return (contact != null) ? contact.getName() : conversation.peer;
            }
        }

        /** Returns the text that should be used as the notification text. */
        public String getText() {
            return (unreadCount > 1) ?
                    mContext.getString(R.string.unread_messages, unreadCount)
                    : conversation.content;
        }

        public long getTimestamp() {
            return conversation.timestamp;
        }

        public PendingIntent getPendingIntent() {
            Intent ni;
            // more than one unread conversation - open ConversationList
            if (convCount > 1) {
                ni = new Intent(mContext, ConversationList.class);
            }
            // one unread conversation - open ComposeMessage on that peer
            else {
                ni = new Intent(mContext, ComposeMessage.class);
                ni.setAction(ComposeMessage.ACTION_VIEW_CONVERSATION);
                ni.putExtra(ComposeMessage.MESSAGE_THREAD_ID, conversation.id);
            }
            return PendingIntent.getActivity(mContext, 0, ni, Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }
}