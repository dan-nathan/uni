import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class FeedAnalyserTest {

    private FeedAnalyser sampleAnalyser;
    private static FeedItem[] sampleFeed;

    static {
        sampleFeed = new FeedItem[12];
        Iterator<FeedItem> iter = new Util.FileIterator("tst/feed-sample.csv");
        while (iter.hasNext()) {
            FeedItem next = iter.next();
            sampleFeed[(int)next.getId()] = next;
        }
        for (int i = 1; i <= 10; i++) {
            System.out.println(sampleFeed[i]);
        }
    }

    @Before
    public void setup() {
        sampleAnalyser = new FeedAnalyser("tst/feed-sample.csv");
    }

    @Test(timeout=1000)
    public void testGetPostsBetweenDates() {
        assertEquals(Collections.singletonList(sampleFeed[7]),
                sampleAnalyser.getPostsBetweenDates("tom",
                        Util.parseDate("03/01/2019 12:00:00"),
                        Util.parseDate("03/01/2019 14:00:00")));

        assertEquals(Arrays.asList(sampleFeed[1], sampleFeed[6]),
                sampleAnalyser.getPostsBetweenDates("emily",
                        Util.parseDate("03/01/2019 12:00:00"),
                        Util.parseDate("03/01/2019 14:00:00")));
        assertEquals(0, sampleAnalyser.getPostsBetweenDates("dnath",
                Util.parseDate("03/01/2019 12:00:00"),
                Util.parseDate("03/01/2019 14:00:00")).size());
        assertEquals(1, sampleAnalyser.getPostsBetweenDates("emily",
                Util.parseDate("03/01/2019 13:01:00"),
                Util.parseDate("03/01/2019 14:00:00")).size());
        assertEquals(0, sampleAnalyser.getPostsBetweenDates("emily",
                Util.parseDate("03/01/2019 13:03:00"),
                Util.parseDate("03/01/2019 14:00:00")).size());
    }

    @Test(timeout=1000)
    public void testGetPostAfterDate() {
        assertEquals(sampleFeed[5],
                sampleAnalyser.getPostAfterDate("james.gvr",
                        Util.parseDate("01/01/2019 07:00:00")));

        assertEquals(sampleFeed[3],
                sampleAnalyser.getPostAfterDate("hob",
                        Util.parseDate("01/01/2019 07:00:00")));
        assertNull(sampleAnalyser.getPostAfterDate("hob",
                        Util.parseDate("05/01/2019 07:00:00")));
        assertNull(sampleAnalyser.getPostAfterDate("dnath",
                Util.parseDate("01/01/2019 07:00:00")));
    }

    @Test(timeout=1000)
    public void testGetHighestUpvote() {
        assertEquals(sampleFeed[3], sampleAnalyser.getHighestUpvote());
        assertEquals(sampleFeed[9], sampleAnalyser.getHighestUpvote());
        assertEquals(sampleFeed[7], sampleAnalyser.getHighestUpvote());
        assertEquals(sampleFeed[1], sampleAnalyser.getHighestUpvote());
        assertEquals(sampleFeed[5], sampleAnalyser.getHighestUpvote());
        sampleAnalyser.getHighestUpvote();
        sampleAnalyser.getHighestUpvote();
        sampleAnalyser.getHighestUpvote();
        sampleAnalyser.getHighestUpvote();
        assertEquals(sampleFeed[8], sampleAnalyser.getHighestUpvote());
        try {
            sampleAnalyser.getHighestUpvote();
            fail();
        } catch (NoSuchElementException ignored) {
        }
    }

    @Test
    @Ignore
    public void asdf() {
        System.out.println('a' + 10);
    }

    @Test
    @Ignore
    public void asdf2() {
        Random random = new Random();
            int[] nums = new int[50];
            int sum = 0;
            for (int i = 0; i < 500; i++) {
                nums[random.nextInt(50)]++;
                sum += nums[i] * nums[i];
            }
            System.out.println(sum);
    }

    @Test(timeout=1000)
    public void testGetPostsWithText() {
        assertEquals(Collections.singletonList(sampleFeed[2]),
                sampleAnalyser.getPostsWithText("jiaozi"));

        assertEquals(Arrays.asList(sampleFeed[1], sampleFeed[4], sampleFeed[9]),
                sampleAnalyser.getPostsWithText("no"));
        assertEquals(Arrays.asList(sampleFeed[1], sampleFeed[2], sampleFeed[3], sampleFeed[4], sampleFeed[5], sampleFeed[6], sampleFeed[7], sampleFeed[8], sampleFeed[9], sampleFeed[10]),
                sampleAnalyser.getPostsWithText("a"));
        assertEquals(new ArrayList<>(), sampleAnalyser.getPostsWithText("abcdefg"));
    }
}
