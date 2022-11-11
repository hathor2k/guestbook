package com.jadecross.guestbook;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
class PostTest {
    @Test
    void testSetName() {
        Post post = new Post("TESTER", "2022-11-11 15:30:48", "Happy Wedding");
        post.setName("DEVOPS");
        assertEquals("DEVOPS", post.getName());
    }
}
