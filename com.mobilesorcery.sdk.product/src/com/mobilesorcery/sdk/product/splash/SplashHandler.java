package com.mobilesorcery.sdk.product.splash;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.AbstractSplashHandler;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.SimpleQueue;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;

public class SplashHandler extends AbstractSplashHandler {

	class LoadProgressMonitor extends NullProgressMonitor implements
			PaintListener {

		private static final int NUM_PROGRESS_STEPS = 60;
		private int totalWork;
		private int worked = 1;
		private GC gc;
		private Display display;
		private Color progressTextColor;
		private Color progressGradientColor1;
		private Color black;
		private String name;

		public void beginTask(String name, int totalWork) {
			this.name = name;
			this.totalWork = totalWork;
			updateUI();
		}

		public void setTaskName(String name) {
			this.name = name;
			updateUI();
		}

		public void subTask(String name) {
			this.name = name;
			updateUI();
		}

		public void worked(int work) {
			internalWorked(work);
		}

		public void internalWorked(double work) {
			this.worked += work;
			updateUI();
		}

		public void done() {
			worked = totalWork;
			updateUI();
		}

		private void updateUI() {
			if (q == null) {
				return;
			}
			q.execute(new Runnable() {
				@Override
				public void run() {
					paintProgress(gc);
				}
			});
		}

		protected void paintProgress(GC gc) {
			int percentage = Math.min(100, totalWork == 0 ? 0 : (100 * worked)
					/ totalWork);
			String percentageStr = totalWork == 0 ? "" : MessageFormat.format(
					"{0}%", percentage);
			Rectangle bounds = gc.getClipping();
			Point textExtent = gc.textExtent(percentageStr);
			gc.setBackground(progressTextColor);
			gc.setForeground(progressGradientColor1);
			int progressWidth = (bounds.width * percentage) / 100;
			int progressHeight = textExtent.y;
			gc.fillGradientRectangle(0, 0, progressWidth, progressHeight, false);
			for (int i = 1; i < NUM_PROGRESS_STEPS; i++) {
				gc.setForeground(progressGradientColor1);
				int lineX = i * bounds.width / NUM_PROGRESS_STEPS;
				if (progressWidth >= lineX) {
					gc.drawLine(lineX, 0, lineX, progressHeight - 1);
				}
			}
			gc.setForeground(progressTextColor);
			/*gc.setBackground(black);
			gc.drawText(percentageStr, bounds.width - textExtent.x,
					progressHeight, false);*/
		}

		public void setGC(GC gc) {
			this.gc = gc;
		}

		public void setDisplay(Display display) {
			this.display = display;
			dispose();
			progressTextColor = new Color(display, new RGB(0xf5, 0x7e, 0x20));
			progressGradientColor1 = new Color(display, new RGB(0, 0, 0));
			black = display.getSystemColor(SWT.COLOR_BLACK);
		}

		public void dispose() {
			if (progressTextColor != null) {
				progressTextColor.dispose();
			}
			if (progressGradientColor1 != null) {
				progressGradientColor1.dispose();
			}
		}

		public void paintControl(PaintEvent event) {
			paintProgress(event.gc);
		}
	}

	private CountDownLatch splashDisposalLatch = new CountDownLatch(1);

	private LoadProgressMonitor progressMonitor;

	private GC shellGC;

	private Shell internalShell;

	private boolean cancelled = false;

	private SimpleQueue q;

	public void init(Shell splash) {
		super.init(splash);
		
		if (Boolean.TRUE.equals(MosyncUIPlugin.getDefault().switchedToExperimental())) {
			return;
		}
		
		// Since we have no control of when the original shell is disposed of,
		// and since we want to always play the animation to the end, this
		// we just create a new shell and hide the old one.
		internalShell = new Shell(splash.getDisplay(), splash.getStyle()
				| SWT.TOOL);
		internalShell.setBackground(splash.getBackground());
		internalShell.setBounds(splash.getBounds());
		internalShell.addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.ESC) {
					splashDisposalLatch.countDown();
				}
			}
		});
		startAnimatedGIF(internalShell);
		internalShell.open();
		splash.setVisible(false);
	}

	private String getSplashResource() {
		return "/splash.gif";
	}

	public IProgressMonitor getBundleProgressMonitor() {
		if (progressMonitor == null) {
			progressMonitor = new LoadProgressMonitor();
		}

		return progressMonitor;
	}

	public void dispose() {
		splashDisposalLatch.countDown();
	}

	public void internalDispose(Display display) {
		try {
			// Never hang on for more then 10 secs.
			splashDisposalLatch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// We can safely ignore this.
		}

		cancelAnimation();

		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					UIUtils.safeDispose(shellGC);
					if (progressMonitor != null) {
						progressMonitor.dispose();
					}
					UIUtils.safeDispose(internalShell);
				}
			});
		}
	}

	public Shell getSplash() {
		return super.getSplash();
	}

	public void startAnimatedGIF(final Shell shell) {
		q = new SimpleQueue(false);

		// Extracted & adapted from snippet 141 (EPL license).
		shell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				// Prevent closing
				event.doit = false;
			}
		});
		final LoadProgressMonitor monitor = (LoadProgressMonitor) getBundleProgressMonitor();
		final Display display = shell.getDisplay();
		final Color shellBackground = shell.getBackground();
		shellGC = new GC(shell);
		monitor.setDisplay(display);
		monitor.setGC(shellGC);
		Runnable animateRunnable = new Runnable() {

			public void run() {
				InputStream animatedGIF = getClass().getResourceAsStream(
						getSplashResource());
				final ImageLoader loader = new ImageLoader();
				final ImageData[] imageDataArray = loader.load(animatedGIF);

				if (imageDataArray.length > 1) {
					Image image = null;

					/*
					 * Create an off-screen image to draw on, and fill it with
					 * the shell background.
					 */
					final Image offScreenImage = new Image(display,
							loader.logicalScreenWidth,
							loader.logicalScreenHeight);
					GC offScreenImageGC = new GC(offScreenImage);
					offScreenImageGC.setBackground(shellBackground);
					offScreenImageGC.fillRectangle(0, 0,
							loader.logicalScreenWidth,
							loader.logicalScreenHeight);

					try {
						/*
						 * Create the first image and draw it on the off-screen
						 * image.
						 */
						int imageDataIndex = 0;
						ImageData imageData = imageDataArray[imageDataIndex];
						UIUtils.safeDispose(image);
						image = new Image(display, imageData);
						offScreenImageGC.drawImage(image, 0, 0,
								imageData.width, imageData.height, imageData.x,
								imageData.y, imageData.width, imageData.height);

						/*
						 * Now loop through the images, creating and drawing
						 * each one on the off-screen image before drawing it on
						 * the shell.
						 */
						int repeatCount = loader.repeatCount;
						while (!cancelled && loader.repeatCount == 0
								|| repeatCount > 0) {
							switch (imageData.disposalMethod) {
							case SWT.DM_FILL_BACKGROUND:
								/*
								 * Fill with the background color before
								 * drawing.
								 */
								Color bgColor = null;
								if (loader.backgroundPixel != -1) {
									bgColor = new Color(
											display,
											imageData.palette
													.getRGB(loader.backgroundPixel));
								}
								offScreenImageGC
										.setBackground(bgColor != null ? bgColor
												: shellBackground);
								offScreenImageGC.fillRectangle(imageData.x,
										imageData.y, imageData.width,
										imageData.height);
								if (bgColor != null)
									bgColor.dispose();
								break;
							case SWT.DM_FILL_PREVIOUS:
								/*
								 * Restore the previous image before drawing.
								 */
								offScreenImageGC.drawImage(image, 0, 0,
										imageData.width, imageData.height,
										imageData.x, imageData.y,
										imageData.width, imageData.height);
								break;
							}

							imageDataIndex = (imageDataIndex + 1)
									% imageDataArray.length;
							imageData = imageDataArray[imageDataIndex];
							UIUtils.safeDispose(image);
							image = new Image(display, imageData);
							offScreenImageGC.drawImage(image, 0, 0,
									imageData.width, imageData.height,
									imageData.x, imageData.y, imageData.width,
									imageData.height);

							monitor.paintProgress(offScreenImageGC);
							/* Draw the off-screen image to the shell. */
							if (!cancelled && !shellGC.isDisposed()) {
								shellGC.drawImage(offScreenImage, 0, 0);
							}

							/*
							 * Sleep for the specified delay time (adding
							 * commonly-used slow-down fudge factors).
							 */
							try {
								int ms = imageData.delayTime * 10;
								if (ms < 20)
									ms += 30;
								if (ms < 30)
									ms += 10;
								Thread.sleep(cancelled ? 0 : ms / 100);
							} catch (InterruptedException e) {
							}

							/*
							 * If we have just drawn the last image, decrement
							 * the repeat count and start again.
							 */
							if (imageDataIndex == imageDataArray.length - 1)
								repeatCount--;
						}
					} catch (Throwable t) {
						CoreMoSyncPlugin.getDefault().log(t);
					} finally {
						UIUtils.safeDispose(offScreenImage);
						UIUtils.safeDispose(offScreenImageGC);
						UIUtils.safeDispose(image);
						Util.safeClose(animatedGIF);
					}
				}
			}
		};
		q.execute(animateRunnable);
		Thread disposeThread = new Thread() {
			public void run() {
				internalDispose(display);				
			}
		};
		disposeThread.setDaemon(true);
		disposeThread.start();
	}

	protected void cancelAnimation() {
		this.cancelled = true;
	}
}
