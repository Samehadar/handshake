package com.samehadar.handshake;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by User on 12.12.2016.
 */
public class MainTest {
    Main main;

    @Before
    public void setUp() throws Exception {
        main = new Main();
    }

    @Test
    public void Should_SeeOpenUserOrNot() {
        main.getUser("466307");
    }
}