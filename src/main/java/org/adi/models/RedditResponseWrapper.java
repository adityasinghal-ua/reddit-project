package org.adi.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditResponseWrapper {
    // the api response looks like this: to handle it, making wrapper class to separate and handle each individual post separately
    /*
    {
  "kind": "Listing",
  "data": {
    "after": null,
    "dist": 1,
    "modhash": "",
    "geo_filter": "",
    "children": [
      {
        "kind": "t3",
        "data": {
          "approved_at_utc": null,
          "subreddit": "u_No-Opinion-936",
          "selftext": "Testing reddit api",
          "author_fullname": "t2_1gczm44ose",
          "saved": false,
          "mod_reason_title": null,
          "gilded": 0,
          "clicked": false,
          "title": "Hi, testing reddit",
          "link_flair_richtext": [],
          "subreddit_name_prefixed": "u/No-Opinion-936",
          "hidden": false,
          "pwls": null,
          "link_flair_css_class": null,
          "downs": 0,
          "thumbnail_height": null,
          "top_awarded_type": null,
          "hide_score": false,
          "name": "t3_1hx863e",
          "quarantine": false,
          "link_flair_text_color": "dark",
          "upvote_ratio": 1,
          "author_flair_background_color": null,
          "subreddit_type": "user",
          "ups": 1,
          "total_awards_received": 0,
          "media_embed": {},
          "thumbnail_width": null,
          "author_flair_template_id": null,
          "is_original_content": false,
          "user_reports": [],
          "secure_media": null,
          "is_reddit_media_domain": false,
          "is_meta": false,
          "category": null,
          "secure_media_embed": {},
          "link_flair_text": null,
          "can_mod_post": false,
          "score": 1,
          "approved_by": null,
          "is_created_from_ads_ui": false,
          "author_premium": false,
          "thumbnail": "self",
          "edited": false,
          "author_flair_css_class": null,
          "author_flair_richtext": [],
          "gildings": {},
          "content_categories": null,
          "is_self": true,
          "mod_note": null,
          "created": 1736410419,
          "link_flair_type": "text",
          "wls": null,
          "removed_by_category": null,
          "banned_by": null,
          "author_flair_type": "text",
          "domain": "self.No-Opinion-936",
          "allow_live_comments": false,
          "selftext_html": "&lt;!-- SC_OFF --&gt;&lt;div class=\"md\"&gt;&lt;p&gt;Testing reddit api&lt;/p&gt;\n&lt;/div&gt;&lt;!-- SC_ON --&gt;",
          "likes": null,
          "suggested_sort": "qa",
          "banned_at_utc": null,
          "view_count": null,
          "archived": false,
          "no_follow": true,
          "is_crosspostable": false,
          "pinned": false,
          "over_18": false,
          "all_awardings": [],
          "awarders": [],
          "media_only": false,
          "can_gild": false,
          "spoiler": false,
          "locked": false,
          "author_flair_text": null,
          "treatment_tags": [],
          "visited": false,
          "removed_by": null,
          "num_reports": null,
          "distinguished": null,
          "subreddit_id": "t5_dbb0ur",
          "author_is_blocked": false,
          "mod_reason_by": null,
          "removal_reason": null,
          "link_flair_background_color": "",
          "id": "1hx863e",
          "is_robot_indexable": true,
          "report_reasons": null,
          "author": "No-Opinion-936",
          "discussion_type": null,
          "num_comments": 0,
          "send_replies": true,
          "contest_mode": false,
          "mod_reports": [],
          "author_patreon_flair": false,
          "author_flair_text_color": null,
          "permalink": "/r/u_No-Opinion-936/comments/1hx863e/hi_testing_reddit/",
          "stickied": false,
          "url": "https://www.reddit.com/r/u_No-Opinion-936/comments/1hx863e/hi_testing_reddit/",
          "subreddit_subscribers": 0,
          "created_utc": 1736410419,
          "num_crossposts": 0,
          "media": null,
          "is_video": false
        }
      }
    ],
    "before": null
  }
}
     */


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Child{
        private RedditPost data;

        public RedditPost getData() {
            return data;
        }

        public void setData(RedditPost data) {
            this.data = data;
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data{
        private List<Child> children;

        public List<Child> getChildren() {
            return children;
        }

        public void setChildren(List<Child> children) {
            this.children = children;
        }
    }

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
