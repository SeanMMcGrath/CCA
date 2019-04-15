import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import javax.imageio.ImageIO;

public class Main {

    /////////////////////////////////////////
    //  Variables
    /////////////////////////////////////////
    public static int parent[];//represents unionfind array
    public static int width;//width of current image
    public static int height;//height of current image
    public static int current_label;//the number that the current label is being set to, 1 since background is white


    /////////////////////////////////////////
    //  Public Classes
    /////////////////////////////////////////

    public static void main(String[] args) throws IOException {
        //take a greyscale images
        //using imageJ turn into binary image

        String picName = "blobs";//picture should already be in greyscale or binary
        String picFormat = ".jpg";//png or jpg work, others do not
        ImagePlus imgPlus = new ImagePlus("C:\\Users\\smm-pc\\Pictures\\ConnectedComponentProject\\" + picName+ picFormat);
        imgPlus = binaryThreshholding(imgPlus);
        FileSaver fs = new FileSaver(imgPlus);
        fs.saveAsJpeg("C:\\Users\\smm-pc\\Pictures\\ConnectedComponentProject\\Binary" + picName + picFormat);//maybe dont need to save? just use img[][] and save later? or both save and use [][]
        ImageProcessor imgProcessor = imgPlus.getProcessor();

        int img[][] = imgProcessor.getIntArray();//binary image in the form of a int double array

        //conduct connected component analysis
        width = imgProcessor.getWidth();
        height = imgProcessor.getHeight();

        //printArray(img, false);
        int labeledImg[][] = classical_with_union_find(img);//labeled for display
        //printArray(labeledImg, true);

        //assign random color to each unique label section
        int[]pixels = new int[width*height];
        Map<Integer, Integer> labelColorMap = new HashMap<>();//<label, color>
        ArrayList<Integer> usedColors = new ArrayList<>();//make sure a;; colors are unique
        //array[width * row + col] = value;  <- double array location to single array location

        Random rand = new Random();
        for (int x = 0; x < width; x++) {//0 is black
            for(int y = 0; y < height; y++) {
                int R;
                int G;
                int B;
                if(labeledImg[x][y] == 0){
                    pixels[(width * y) + x] = 16777215; //make white since its the background
                } else {
                    if(!labelColorMap.containsKey(labeledImg[x][y])) {
                        int color;//int representation of the color at pixel[(width * x) + y]
                        do {
                            R = rand.nextInt(256);
                            G = rand.nextInt(256);
                            B = rand.nextInt(256);
                            color = R;
                            color = (color << 8) + G;//left bits are red, middle green, and right blue
                            color = (color << 8) + B;
                        } while(usedColors.contains(color));
                        usedColors.add(color);
                        labelColorMap.put(labeledImg[x][y], color);
                        pixels[(width * y) + x] = color;
                    } else {
                        pixels[(width * y) + x] = labelColorMap.get(labeledImg[x][y]);
                    }
                }
            }
        }
        System.out.println("Connected Components: " + usedColors.size());//output how man connected components there are


        //save as new image
        ColorProcessor colorP = new ColorProcessor(width, height, pixels);
        Image i = colorP.createImage();
        BufferedImage bi = new BufferedImage(i.getWidth(null), i.getHeight(null), BufferedImage.TYPE_INT_RGB);
        bi.getGraphics().drawImage(i,0,0, null);
        ImageIO.write(bi, "JPG", new File("C:\\Users\\smm-pc\\Pictures\\ConnectedComponentProject\\Color" + picName + ".jpg"));

        //open that file
        File f = new File("C:\\Users\\smm-pc\\Pictures\\ConnectedComponentProject\\Color" + picName + ".jpg");
        Desktop dt = Desktop.getDesktop();
        dt.open(f);
    }

    //creates a binary image using simple binaryThreshholding
    public static ImagePlus binaryThreshholding(ImagePlus i) {
        ImageProcessor imgProcessor = i.getProcessor();
        int img[][] = imgProcessor.getIntArray();

        for (int x = 0; x < imgProcessor.getWidth(); x++) {//simple binaryThreshholding into a binary picture
            for (int y = 0; y < imgProcessor.getHeight(); y++) {
                if (img[x][y] > 127) {// < to inverse picture
                    img[x][y] = 255;
                } else {
                    img[x][y] = 0;
                }
            }
        }
        imgProcessor.setIntArray(img);
        ImagePlus imgPlus = new ImagePlus("placeholder", imgProcessor);

        return imgPlus;
    }

    // initialize global variable label and array PARENT
    public static void initialize() {
        parent = new int[width * height];
        current_label = 1;

        for (int i = 0; i < width * height; i++) {
            parent[i] = 0;//i think this part of the psuedo-code is irrelevent
        }//pretty sure arrays initially start at 0
    }

    // B - original binary image
    // LB - labeled connected component image
    // program assumes that one is background(white) and zero is objects to be detected(black)
    // computes the connected components of a binary image
    // NOTE: as expected for IMG[i][j], IMG[0][0] is the top left of image, IMG[width][height] is the bottom right corner, IMG[0][height] is the bottom left, and IMG[width][0] is the top right
    public static int[][] classical_with_union_find(int B[][]) {
        initialize();
        int[][] LB = new int[width][height];

        //pass 1 assigns initial labels to each row L of the image
        for(int L = 0; L < width; L++){
            //initialize all labels on line L to zero
            for (int P = 0; P < height; P++) {
                LB[L][P] = 0;
            }
            //process line L.
            for (int P = 0; P < height; P++) {
                if (B[L][P] == 0) {//if its black, not background
                    int A[] = new int[2];//2 if 4 directional, 4 if 8 directional. I am using 4 directional in this case.
                    int Aindex = 0;
                    int M = 0;
                    //A = prior_neighbors(L, P)
                    //set A to be an array of all neighbors that are not background
                    if(L > 0 && B[L-1][P] == 0) {//not on L axis border, and not background
                        A[Aindex] = LB[L-1][P];//get the label at the neighboring position
                        M = LB[L-1][P];
                        Aindex++;
                    }
                    if(P > 0 && B[L][P-1] == 0){//not on P axis border, and not background
                        A[Aindex] = LB[L][P-1];
                        if(M == 0 || M > LB[L][P-1]) {//if M is unset, or this new label is smaller than the older recorded label, set M to be this new label
                            M = LB[L][P - 1];
                        }
                        Aindex++;
                    }

                    if (Aindex == 0){//no neighbors so set M to the most current label number and increment
                        M = current_label;
                        current_label++;
                    }
                    LB[L][P] = M;
                    if(L > 0 && Aindex != 0 && B[L-1][P] == B[L][P] && M != LB[L-1][P]){//labels to the left are different even though their connected, so do union
                        union(M, LB[L-1][P]);
                    }
                    if(P > 0 && Aindex != 0 && B[L][P-1] == B[L][P] && M != LB[L][P-1]){//labels upward(down) are different even though their connected, so do union
                        union(M, LB[L][P-1]);
                    }
                }
            }
        }

        //pass 2 replaces pass 1 labels with equivalence class labels
            for (int L = 0; L < width; L++) {
                for (int P = 0; P < height; P++) {
                    if (B[L][P] == 0) {//if not background
                        LB[L][P] = find(LB[L][P]);
                    }
                }
            }
        return LB;
    }

    // x - label of first set
    // y - label of second set
    // parent - global array representing unionfind data structure
    // constructs the union of the two labels, using the lower one
    public static void union(int x, int y) {
        int j = x;
        int k = y;
        while (parent[j] != 0) j = parent[j];
        while (parent[k] != 0) k = parent[k];
        if (j != k) {
            if (x > y)//smaller one is kept
                parent[x] = y;
            else parent[y] = x;
        }
    }

    // x - a label of a set
    // parent - global array representing unionfind data structure
    // finds the parent label of a set
    public static int find(int x) {
        int j = x;
        while (parent[j] != 0) j = parent[j];
        return j;
    }

    //for testing purposes
    //prints out int[][] array to look similar to image
    //if the label# is greater than 9, for the sake of not messing up the picture it is randomly set to a # between 1 and 9
    public static void printArray(int[][] i, boolean labeled) {
        System.out.println(width);
        System.out.println(height);
        if(!labeled) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (i[x][y] == 255) System.out.print(1);
                    else if (i[x][y] == 0) System.out.print(0);
                    else System.out.println("ERROR ERROR ERROR<" + i[x][y] + ">");
                }
                System.out.println("");//might need adjusting
            }
        } else {
            ArrayList<Integer> temp = new ArrayList<>();
            Map<Integer, Integer> randomAssigner = new HashMap<>();
            Random rand = new Random();//z = rand.nextInt(9); z++; from 1-9

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if(i[x][y] == 0){//if 0 its background
                        System.out.print(0);
                    } else {
                        if (i[x][y] > 9){
                            if(randomAssigner.containsKey(i[x][y])){
                                System.out.print(randomAssigner.get(i[x][y]));
                            } else {
                                int z = rand.nextInt(9);
                                z++;
                                randomAssigner.put(i[x][y], z);
                                System.out.print(z);
                            }
                            temp.add(i[x][y]);
                        } else {
                            System.out.print(i[x][y]);
                        }
                    }
                }
                System.out.println();
            }
            for (Integer x : temp) {
                System.out.println(x);
            }
        }
    }
}


