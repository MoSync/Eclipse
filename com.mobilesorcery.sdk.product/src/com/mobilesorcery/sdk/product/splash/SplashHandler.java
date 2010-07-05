package com.mobilesorcery.sdk.product.splash;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.deferred.SetModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.AbstractSplashHandler;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.UIUtils;

public class SplashHandler extends AbstractSplashHandler {

    class LoadProgressMonitor extends NullProgressMonitor {
        
        private static final int NUM_PROGRESS_STEPS = 60;
        private int totalWork;
        private int worked = 1;
        private GC gc;
        private Display display;
        private Color progressTextColor;
        private Color progressGradientColor1;
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
            if (display != null && gc != null) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        paintProgress(gc);
                    }   
                });
            }
        }
        
        protected void paintProgress(GC gc) {
            int percentage = Math.min(100, totalWork == 0 ? 0 : (100 * worked) / totalWork);
            String percentageStr = totalWork == 0 ? "" : MessageFormat.format("{0} -- {1}%", name, percentage);
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
                    gc.drawLine(lineX, 0, lineX, progressHeight);
                }
            }
            gc.setForeground(progressTextColor);
            gc.drawText(percentageStr, bounds.width - textExtent.x, progressHeight, true);
        }

        public void setGC(GC gc) {
            this.gc = gc;
        }
        
        public void setDisplay(Display display) {
            this.display = display;
            dispose();
            progressTextColor = new Color(display, new RGB(0xf5, 0x7e, 0x20));
            progressGradientColor1 = new Color(display, new RGB(0, 0, 0)/*new RGB(0x5f / 2, 0x7e / 2, 0x20 / 2)*/);
        }
        
        public void dispose() {
            if (progressTextColor != null) {
                progressTextColor.dispose();
            }
            if (progressGradientColor1 != null) {
                progressGradientColor1.dispose();
            }
        }
    }
    
    private CountDownLatch splashDisposalLatch = new CountDownLatch(1);
    
    private LoadProgressMonitor progressMonitor;

    private GC shellGC;

    public void init(Shell splash) {
        super.init(splash);
        startAnimatedGIF(splash);
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
            splashDisposalLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // We can safely ignore this.
        }
        
        display.asyncExec(new Runnable() {
            public void run() {
                if (shellGC != null) {
                    shellGC.dispose();
                }
                
                if (progressMonitor != null) {
                    progressMonitor.dispose();
                }

                getSplash().close();
            }            
        });
    }
    
    public void startAnimatedGIF(Shell shell) {
        // Extracted & adapted from snippet 141 (EPL license).
        final ImageLoader loader = new ImageLoader();
        shellGC = new GC(shell);
        final LoadProgressMonitor monitor = (LoadProgressMonitor) getBundleProgressMonitor();
        final Display display = shell.getDisplay();
        final Color shellBackground = shell.getBackground();
        monitor.setDisplay(display);
        monitor.setGC(shellGC);
        InputStream animatedGIF = getClass().getResourceAsStream(getSplashResource());
        try {
          final ImageData[] imageDataArray = loader.load(animatedGIF);
          if (imageDataArray.length > 1) {
            Thread animateThread = new Thread("Animation") {
              public void run() {
                Image image = null;
                
                /* Create an off-screen image to draw on, and fill it with the shell background. */
                Image offScreenImage = new Image(display, loader.logicalScreenWidth, loader.logicalScreenHeight);
                GC offScreenImageGC = new GC(offScreenImage);
                offScreenImageGC.setBackground(shellBackground);
                offScreenImageGC.fillRectangle(0, 0, loader.logicalScreenWidth, loader.logicalScreenHeight);
                  
                try {
                  /* Create the first image and draw it on the off-screen image. */
                  int imageDataIndex = 0;  
                  ImageData imageData = imageDataArray[imageDataIndex];
                  if (image != null && !image.isDisposed()) image.dispose();
                  image = new Image(display, imageData);
                  offScreenImageGC.drawImage(
                    image,
                    0,
                    0,
                    imageData.width,
                    imageData.height,
                    imageData.x,
                    imageData.y,
                    imageData.width,
                    imageData.height);

                  /* Now loop through the images, creating and drawing each one
                   * on the off-screen image before drawing it on the shell. */
                  int repeatCount = loader.repeatCount;
                  while (loader.repeatCount == 0 || repeatCount > 0) {
                    switch (imageData.disposalMethod) {
                    case SWT.DM_FILL_BACKGROUND:
                      /* Fill with the background color before drawing. */
                      Color bgColor = null;
                      if (loader.backgroundPixel != -1) {
                        bgColor = new Color(display, imageData.palette.getRGB(loader.backgroundPixel));
                      }
                      offScreenImageGC.setBackground(bgColor != null ? bgColor : shellBackground);
                      offScreenImageGC.fillRectangle(imageData.x, imageData.y, imageData.width, imageData.height);
                      if (bgColor != null) bgColor.dispose();
                      break;
                    case SWT.DM_FILL_PREVIOUS:
                      /* Restore the previous image before drawing. */
                      offScreenImageGC.drawImage(
                        image,
                        0,
                        0,
                        imageData.width,
                        imageData.height,
                        imageData.x,
                        imageData.y,
                        imageData.width,
                        imageData.height);
                      break;
                    }
                              
                    imageDataIndex = (imageDataIndex + 1) % imageDataArray.length;
                    imageData = imageDataArray[imageDataIndex];
                    image.dispose();
                    image = new Image(display, imageData);
                    offScreenImageGC.drawImage(
                      image,
                      0,
                      0,
                      imageData.width,
                      imageData.height,
                      imageData.x,
                      imageData.y,
                      imageData.width,
                      imageData.height);
                    
                    /* Draw the off-screen image to the shell. */
                    shellGC.drawImage(offScreenImage, 0, 0);
                    monitor.paintProgress(shellGC);
                    
                    /* Sleep for the specified delay time (adding commonly-used slow-down fudge factors). */
                    try {
                      int ms = imageData.delayTime * 10;
                      if (ms < 20) ms += 30;
                      if (ms < 30) ms += 10;
                      Thread.sleep(ms);
                    } catch (InterruptedException e) {
                    }
                    
                    /* If we have just drawn the last image, decrement the repeat count and start again. */
                    if (imageDataIndex == imageDataArray.length - 1) repeatCount--;
                  }
                } catch (SWTException e) {
                  System.out.println("There was an error animating the GIF");
                  e.printStackTrace();
                } finally {
                  if (offScreenImage != null && !offScreenImage.isDisposed()) offScreenImage.dispose();
                  if (offScreenImageGC != null && !offScreenImageGC.isDisposed()) offScreenImageGC.dispose();
                  if (image != null && !image.isDisposed()) image.dispose();
                  internalDispose(display);
                }
              }
            };
            animateThread.setDaemon(true);
            animateThread.setName("Splash animation");
            animateThread.start();
          }
        } finally {
            Util.safeClose(animatedGIF);
        }
    }
}
