package com.flleeppyy.serverinstaller.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {
    /**
     * Resize an image to the specified dimension
     * @param imageBytes
     * @param size
     * @return byte[]
     */
    public static byte[] resizeImage(byte[] imageBytes, Dimension size) throws IOException {
        ImageIcon ico = new ImageIcon(imageBytes);
        BufferedImage image = imageToBufferedImage(ico.getImage());
        BufferedImage ret = new BufferedImage(size.width, size.height, BufferedImage.TRANSLUCENT);
        ret.getGraphics().drawImage(image, 0, 0, size.width, size.height, null);

        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            ImageIO.write(ret, "png", baos);
        } finally {
            try {
                assert baos != null;
                baos.close();
            } catch (Exception ignored) {
            }
        }
        return baos.toByteArray();

    }

    // stupid cause enums dont exist yet
    private static class dimension {
        public static int x = 0;
        public static  int y = 1;
    }

    /**
     * Resizes an image to the width/height of @dim, based on which dimension is bigger
     * @param imageBytes The bytes of the image
     * @param size the pixels you want to resize the image to
     * @return byte[]
     */
    public static byte[] resizeToFitSquareDim(byte[] imageBytes, int size) throws IOException {
        Dimension dim = getImageDimensions(imageBytes);
        int greaterDim;
        if (dim.width > dim.height) {
            greaterDim = dimension.x;
        } else {
            greaterDim = dimension.y;
        }

        int newSize = 0;
        if (greaterDim == dimension.x) {
            newSize = size;
        } else {
            newSize = (int) (size * ((double) dim.width / dim.height));
        }

        return resizeImage(imageBytes, new Dimension(newSize, newSize));
    }



    public static Dimension getImageDimensions(byte[] imageBytes) {
        ImageIcon ico = new ImageIcon(imageBytes);
        return new Dimension(ico.getIconWidth(), ico.getIconHeight());
    }

    public static BufferedImage imageToBufferedImage(Image im) {
        BufferedImage bi = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics bg = bi.getGraphics();
        bg.drawImage(im, 0, 0, null);
        bg.dispose();
        return bi;
    }
}
