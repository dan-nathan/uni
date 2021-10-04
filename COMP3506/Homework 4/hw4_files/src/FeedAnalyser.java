import java.util.*;

/**
 * Class that implements the social media feed searches
 */
public class FeedAnalyser {

    // Map from a username to that user's posts (ordered by date)
    private Map<String, ArrayList<FeedItem>> postsByDate;

    // Posts ordered by the number of upvotes
    private List<FeedItem> postsByUpvote;
    private int upvoteIndex;

    private List<FeedItem> postsByID;

    /**
     * Loads social media feed data from a file
     *
     * @param filename the file to load from
     */
    public FeedAnalyser(String filename) {
        Iterator<FeedItem> iter = new Util.FileIterator(filename);
        postsByDate = new HashMap<>();
        postsByUpvote = new ArrayList<>();
        postsByID = new ArrayList<>();
        while (iter.hasNext()) {
            FeedItem item = iter.next();
            ArrayList<FeedItem> userPosts = postsByDate.get(item.getUsername());
            // If this is the first post by the user
            if (userPosts == null) {
                // Create new ArrayList, add item, and add it to map
                userPosts = new ArrayList<>();
                userPosts.add(item);
                postsByDate.put(item.getUsername(), userPosts);
            } else {
                userPosts.add(item);
            }
            postsByUpvote.add(item);
            postsByID.add(item);
        }
        // Sort all lists by relevant fields
        for (List<FeedItem> userPosts : postsByDate.values()) {
            userPosts.sort(Comparator.comparing(FeedItem::getDate));
        }
        postsByUpvote.sort(Comparator.comparing(FeedItem::getUpvotes));
        upvoteIndex = postsByUpvote.size() - 1;
        postsByID.sort(Comparator.comparing(FeedItem::getId));
    }

    /*
     * A binary search algorithm that uses recursion find the first post after
     * a certain date. This method assumes that the list of posts is already
     * sorted by date, and will return a seemingly random index otherwise
     */
    private int binarySearchDate(List<FeedItem> posts, Date date, int start, int end) {
        if (start == end) {
            // If the date is after the date at this index, return the next
            // index
            if (date.compareTo(posts.get(start).getDate()) > 0) {
                return start + 1;
            } else {
                return start;
            }
        }
        int middle = (start + end) / 2;
        if (date.compareTo(posts.get(middle).getDate()) > 0) {
            // If the date is after the middle date, search after it
            return binarySearchDate(posts, date, middle + 1, end);
        } else if (date.compareTo(posts.get(middle).getDate()) < 0) {
            // If the date is after the middle date, search before it
            return binarySearchDate(posts, date, start, middle);
        } else {
            return middle;
        }
    }

    /**
     * Return all feed items posted by the given username between startDate and endDate (inclusive)
     * If startDate is null, items from the beginning of the history should be included
     * If endDate is null, items until the end of the history should be included
     * The resulting list should be ordered by the date of each FeedItem
     * If no items that meet the criteria can be found, the empty list should be returned
     *
     * @param username the user to search the posts of
     * @param startDate the date to start searching from
     * @param endDate the date to stop searching at
     * @return a list of FeedItems made by username between startDate and endDate
     *
     * @require username != null
     * @ensure result != null
     */
    public List<FeedItem> getPostsBetweenDates(String username, Date startDate, Date endDate) {
        List<FeedItem> userPosts = postsByDate.get(username);
        if (userPosts == null) {
            return new ArrayList<>();
        }
        int startIndex;
        if (startDate == null) {
            // Start search from start of list
            startIndex = 0;
        } else {
            startIndex = binarySearchDate(userPosts, startDate, 0, userPosts.size() - 1);
        }
        if (startIndex == userPosts.size()) {
            return new ArrayList<>();
        }

        int endIndex;
        if (endDate == null) {
            // Search until the end of the list
            endIndex = userPosts.size();
        } else {
            endIndex = binarySearchDate(userPosts, endDate, 0, userPosts.size() - 1);
            // subList has an exclusive end index, meaning the 1 should be added to the index
            // if the current end index is on the exact date (otherwise end index would already
            // be the next post after end date)
            if (endIndex < userPosts.size() && userPosts.get(endIndex).getDate() == endDate) {
                endIndex++;
            }
        }
        return userPosts.subList(startIndex, endIndex);
    }

    /**
     * Return the first feed item posted by the given username at or after searchDate
     * That is, the feed item closest to searchDate that is greater than or equal to searchDate
     * If no items that meet the criteria can be found, null should be returned
     *
     * @param username the user to search the posts of
     * @param searchDate the date to start searching from
     * @return the first FeedItem made by username at or after searchDate
     *
     * @require username != null && searchDate != null
     */
    public FeedItem getPostAfterDate(String username, Date searchDate) {
        List<FeedItem> userPosts = postsByDate.get(username);
        // Return null if the user hasn't made a post
        if (userPosts == null) {
            return null;
        }
        // Find the first index on or after the date for the username
        int index = binarySearchDate(userPosts, searchDate, 0, userPosts.size() - 1);
        if (index == userPosts.size()) {
            return null;
        }
        return userPosts.get(index);
    }

    /**
     * Return the feed item with the highest upvote
     * Subsequent calls should return the next highest item
     *     i.e. the nth call to this method should return the item with the nth highest upvote
     * Posts with equal upvote counts can be returned in any order
     *
     * @return the feed item with the nth highest upvote value,
     *      where n is the number of calls to this method
     * @throws NoSuchElementException if all items in the feed have already been returned
     *      by this method
     */
    public FeedItem getHighestUpvote() throws NoSuchElementException {
        if (upvoteIndex < 0) {
            throw new NoSuchElementException();
        }
        return postsByUpvote.get(upvoteIndex--);
    }


    /**
     * Return all feed items containing the specific pattern in the content field
     * Case should not be ignored, eg. the pattern "hi" should not be matched in the text "Hi there"
     * The resulting list should be ordered by FeedItem ID
     * If the pattern cannot be matched in any content fields the empty list should be returned
     *
     * @param pattern the substring pattern to search for
     * @return all feed items containing the pattern string
     *
     * @require pattern != null && pattern.length() > 0
     * @ensure result != null
     */
    public List<FeedItem> getPostsWithText(String pattern) {
        // There are 95 valid characters - ASCII values 32 to 126 inclusive
        int[] lastOccurenceFn = new int[95];

        for (int i = 0; i < pattern.length(); i++) {
            int character = pattern.charAt(i);
            // Assign to i + 1 so that the first character has index of 1 (while characters not
            // present remain 0). There are 32 ASCII values below the first valid character
            lastOccurenceFn[character - 32] = i + 1;
        }

        List<FeedItem> postsWithText = new ArrayList<>();

        for (FeedItem item : postsByID) {
            String content = item.getContent();
            // Start matching from the end of the pattern
            int contentIndex = pattern.length() - 1;
            int patternIndex = pattern.length() - 1;
            int foundIndex = -1;
            // Boyer-Moore algorithm
            while (contentIndex < content.length()) {
                if (content.charAt(contentIndex) == pattern.charAt(patternIndex)) {
                    if (patternIndex == 0) {
                        // If the pattern has matched all the way to the start, record the index
                        // and exit the loop
                        foundIndex = contentIndex;
                        break;
                    } else {
                        // Check the previous character next time
                        contentIndex--;
                        patternIndex--;
                    }
                } else {
                    // Pattern hasn't matched - jump forwards
                    int lastOccurence = lastOccurenceFn[(int)content.charAt(contentIndex) - 32];
                    // when assigning the array, lastOccurenceFn already had 1 added to it, so
                    // don't add 1 here
                    contentIndex += pattern.length() - Math.min(patternIndex, lastOccurence);
                    patternIndex = pattern.length() - 1;
                }
            }
            if (foundIndex != -1) {
                postsWithText.add(item);
            }
        }

        return postsWithText;
    }
}
