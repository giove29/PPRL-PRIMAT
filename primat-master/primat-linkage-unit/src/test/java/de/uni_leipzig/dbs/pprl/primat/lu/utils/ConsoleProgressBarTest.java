package de.uni_leipzig.dbs.pprl.primat.lu.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

public class ConsoleProgressBarTest {

	@Test
	void nonInteractivePrintsOneLinePerDecileAndReachesHundredPercent() {
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		final ConsoleProgressBar bar = new ConsoleProgressBar("Test", new PrintStream(buffer), false);

		for (int i = 0; i <= 100; i++) {
			bar.update(i, 100);
		}

		final String[] lines = buffer.toString().trim().split("\\R");
		assertEquals(11, lines.length);
		assertTrue(lines[0].contains("  0%|"));
		assertTrue(lines[10].contains("100%|" + "#".repeat(30) + "| 100/100"));
	}

	@Test
	void zeroTotalCompletesImmediately() {
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		final ConsoleProgressBar bar = new ConsoleProgressBar("Empty", new PrintStream(buffer), true);

		bar.update(0, 0);

		assertTrue(buffer.toString().contains("100%"));
		assertTrue(buffer.toString().endsWith(System.lineSeparator()));
	}
}
