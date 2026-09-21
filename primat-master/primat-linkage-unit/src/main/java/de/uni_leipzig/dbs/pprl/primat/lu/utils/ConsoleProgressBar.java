/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.utils;

import java.io.PrintStream;

/**
 * Barra di progresso da console in stile tqdm. Con un terminale interattivo
 * riscrive la stessa riga con {@code \r}; con output rediretto stampa invece
 * una riga ogni 10%.
 */
public class ConsoleProgressBar implements ProgressListener {

	private static final int WIDTH = 30;
	private static final long MIN_REPAINT_NANOS = 100_000_000L;

	private final String label;
	private final PrintStream out;
	private final boolean interactive;
	private long startNanos = -1;
	private long lastPaintNanos;
	private int lastDecile = -1;
	private boolean finished;

	public ConsoleProgressBar(String label) {
		this(label, System.out, System.console() != null);
	}

	public ConsoleProgressBar(String label, PrintStream out, boolean interactive) {
		this.label = label;
		this.out = out;
		this.interactive = interactive;
	}

	@Override
	public synchronized void update(long done, long total) {
		if (finished) {
			return;
		}
		final long now = System.nanoTime();
		if (startNanos < 0) {
			startNanos = now;
		}
		final boolean complete = total <= 0 || done >= total;
		final int percent = total <= 0 ? 100 : (int) Math.min(100, done * 100 / total);

		if (interactive) {
			if (!complete && now - lastPaintNanos < MIN_REPAINT_NANOS) {
				return;
			}
			lastPaintNanos = now;
			out.print("\r" + render(done, total, percent, now));
			if (complete) {
				out.println();
			}
		}
		else {
			final int decile = percent / 10;
			if (decile > lastDecile) {
				lastDecile = decile;
				out.println(render(done, total, percent, now));
			}
		}
		finished = complete;
	}

	String render(long done, long total, int percent, long nowNanos) {
		final double elapsed = (nowNanos - startNanos) / 1e9;
		final double rate = elapsed > 0 ? done / elapsed : 0;
		final double eta = rate > 0 && total > done ? (total - done) / rate : 0;
		final int filled = total <= 0 ? WIDTH : (int) (WIDTH * Math.min(done, total) / total);
		final StringBuilder bar = new StringBuilder();
		for (int i = 0; i < WIDTH; i++) {
			bar.append(i < filled ? '#' : '-');
		}
		return String.format("%-14s %3d%%|%s| %d/%d [%s<%s, %.0f it/s]", label, percent, bar, done, total,
			formatTime(elapsed), formatTime(eta), rate);
	}

	private static String formatTime(double seconds) {
		final long s = (long) seconds;
		return String.format("%02d:%02d", s / 60, s % 60);
	}
}
