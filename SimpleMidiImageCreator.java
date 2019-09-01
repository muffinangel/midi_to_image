import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleMidiImageCreator {

    public static final int imageSize= 128;

    /**
     *
     * @param simpleNoteList - already filtered list of notes
     * @param pathToFile - should be png
     * @param start - start time
     * @param end - end time
     * @param noteTimeScale - how to scale time
     */
    public static void createSimplePianoRollFromNotes(List<NoteInformation> simpleNoteList, String pathToFile,
                                             int start, int end, Double noteTimeScale) {

        // create the picture
        BufferedImage bufferedImage = new BufferedImage(imageSize , imageSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        // fill all the image with white
        g2d.setColor(Color.black);
        g2d.fillRect(0, 0,imageSize, imageSize);

        System.out.println(pathToFile);
        // fill reclangles of simple notes
        for(var simpleNote: simpleNoteList) {
            // get the height position
            int height = simpleNote.key;
            int volacity = simpleNote.velocity;
            Integer volacityAtEnd = simpleNote.velocityAtEnd;
            if(volacityAtEnd == null)
                volacityAtEnd = 0;
            Color color = new Color(volacity, simpleNote.tempo % 255, volacityAtEnd);
            g2d.setColor(color);
            // TODO: do some notes validations
            int startTime = (int) Math.round(simpleNote.getStartTimeInMilliSeconds() * noteTimeScale);
            int endTime =(int) Math.round((simpleNote.getEndTimeInMilliSeconds() * noteTimeScale));
            if(endTime != startTime && startTime < endTime)
                System.out.println(height + "(" + startTime + "/" + simpleNote.startTime + ":" + endTime + ")");
//            System.out.print(", ");


            g2d.fillRect((int) (startTime - start), height, (int) (endTime - startTime) + 1, 1);
        }
        System.out.println(" ");
        g2d.dispose();

        // Save as PNG
        File file = new File(pathToFile); // TODO save PPQ information in a file name
        try {
            ImageIO.write(bufferedImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static List<NoteInformation> createNotesFromSimplePianoRoll(String pathToFile) {
        File file = new File(pathToFile);
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        float PPQ = 480; // TODO delete PPQ
        float divisionTime = 0; // TODO read those from filename
        float resolution = PPQ;

        List<NoteInformation> notes = new ArrayList<>();
        var guard = new NoteInformation(null, null, null, null, null, null, 0,
                null, null, 0, 0);
        notes.add(guard);

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        for(var y = 0; y < height; y++) {
            for(var x = 0; x < width; x++) {
                int clr = bufferedImage.getRGB(x,y);
                int  red   = (clr & 0x00ff0000) >> 16;
                int  green = (clr & 0x0000ff00) >> 8;
                int  blue  =  clr & 0x000000ff;
                var noteToCompare = notes.get(notes.size() - 1);
                if(red != 0 || green != 0 || blue != 0) { // tempo can't be 0 and volacity shouldnt be 0
                    int volacity = red;
                    int tempo = green; // TODO only < 255 values
                    int volacityAtEnd = blue;


                    if(!areValuesEqual(noteToCompare, volacity, tempo, volacityAtEnd, y)) {
                        // Time to add a new note and maybe complete the previous
                        var newNote = new NoteInformation(null, null, null,
                                y, volacity, volacityAtEnd, PPQ, tempo, null, divisionTime, resolution);
                        newNote.startTime = newNote.convertToTicks(Double.valueOf(x));
                        notes.add(newNote);
                    }
                }

                completeNoteIfNeccessary(noteToCompare, x, y);
            }
        }

        notes.remove(guard);
        return notes;
    }

    private static boolean areValuesEqual(NoteInformation noteToCompare, int volacity, int tempo, int volacityAtEnd, int y) {
        Integer noteVolacity = noteToCompare.velocity;
        Integer noteTempo = noteToCompare.tempo;
        Integer noteVelocityAtEnd = noteToCompare.velocityAtEnd;
        Integer noteKey = noteToCompare.key;
        Long startTime = noteToCompare.startTime;

        if(noteVolacity == null || noteTempo == null || noteVelocityAtEnd == null || noteKey == null || startTime == null )
            return false;

        if(noteKey.intValue() != y)
            return false;

        if(noteVolacity.intValue() != volacity || noteTempo != tempo || noteVelocityAtEnd != volacityAtEnd)
            return false;

        Long endTime = noteToCompare.endTime;
        if(endTime != null) // this note was completed
            return false;

        return true;
    }

    private static void completeNoteIfNeccessary(NoteInformation note, int x, int y) {
        Long endTime = note.endTime;
        if(endTime != null || note.key == null) return;

        if(y != note.key) { // we are in a different line, so end time should be imageSize - 1
            note.endTime = note.convertToTicks(Double.valueOf(imageSize - 1));
        } else {
            note.endTime = note.convertToTicks(Double.valueOf(x - 1));
        }
    }
}
