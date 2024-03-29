
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHelper {
    public static ArrayList<String> fileNames = new ArrayList<>();
    public static enum DIVIDING_OPTION {
        NORMAL,
        TIME_OVERLAPPING
    }

    private static DIVIDING_OPTION option = DIVIDING_OPTION.NORMAL;

    public static void setDividingOption(DIVIDING_OPTION opt) {
        option = opt;
    }

    public static DIVIDING_OPTION getDividingOption() {
        return option;
    }

    public static void loadFiles(String path) {
        try (Stream<Path> walk = Files.walk(Paths.get(path))) {

            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            // TODO check extensions here?

            fileNames.addAll(result);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void divideFiles(double scale, String pathToFolder) {
        for(String fileName: fileNames) {
            NoteProvider noteProvider = new NoteProvider();
            int lastSlash = fileName.lastIndexOf("\\");
            String name = fileName;
            if(lastSlash >= 0) {
                name = fileName.substring(lastSlash + 1);
            }
            int lastDot = name.lastIndexOf(".");
            if(lastDot >= 0) {
                String ext = name.substring(lastDot + 1);
                if(ext.equals("mid")) {
                    name = name.substring(0, lastDot);
                    System.out.println(name);
                    var notes = noteProvider.getMidiNotes(fileName);
                    if (option == DIVIDING_OPTION.NORMAL)
                        divideFile(noteProvider, notes, scale, pathToFolder, name);
                    else if(option == DIVIDING_OPTION.TIME_OVERLAPPING)
                        divideFileTimeOverlapping(noteProvider, notes, scale, 0.8, 8,pathToFolder, name);
                }
            }
        }
    }

    private static void divideFile(NoteProvider noteProvider, Map<Integer, List<NoteInformation>> notes, double scale, String folder, String fileName) {
        for(var channel: notes.keySet()) {
            int counter = 0;
            boolean stop = false;
            int start = 0;
            int end = 128;
            // assume that the cut ending shouldn't be treated as a valid input
            do {
                var notesFiltered = noteProvider.filterNotes(notes.get(channel), start, end - 1, scale,  true);
                int notesFilteredStart = start;
                int notesFilteredEnd = end;
                start += 128;
                end += 128;
                var nextNotesFiltered = noteProvider.filterNotes(notes.get(channel), start, end - 1, scale,  true);
                int nextStart = start;
                int nextEnd = end;
                start += 128;
                end += 128;
                if(nextNotesFiltered.isEmpty()) {
                    stop = true;
                } else {
                    if(notesFiltered.size() > 5)
                    SimpleMidiImageCreator.createSimplePianoRollFromNotes(
                            notesFiltered, folder + "\\" + fileName + "_ch" + channel + "_" + counter++ + ".png",
                            notesFilteredStart, notesFilteredEnd - 1, scale);
                    if(nextNotesFiltered.size() > 5)
                    SimpleMidiImageCreator.createSimplePianoRollFromNotes(
                            nextNotesFiltered, folder + "\\" + fileName + "_ch" + channel + "_" + counter++ + ".png",
                            nextStart, nextEnd - 1, scale);
                }
            } while(!stop);
        }
    }

    public static void divideFileTimeOverlapping(NoteProvider noteProvider, Map<Integer, List<NoteInformation>> notes, double scale, double prevTimeProportion, int minimalNumberOfNotes, String folder, String fileName) {
        for(var channel: notes.keySet()) {
            boolean stop = false;
            int start = 0;
            int end = 128;
            int counter = 0; // for the fileNames
            int timeStep = (int) (128*prevTimeProportion);

            do {
                var notesFiltered = noteProvider.filterNotes(notes.get(channel), start, end - 1, scale,  true);
                int notesFilteredStart = start;
                int notesFilteredEnd = end;
                start += timeStep;
                end += timeStep;
                var nextNotesFiltered = noteProvider.filterNotes(notes.get(channel), start, end - 1, scale,  true);
                int nextStart = start;
                int nextEnd = end;
                start += timeStep;
                end += timeStep;
                if(nextNotesFiltered.isEmpty() || nextNotesFiltered.size() < minimalNumberOfNotes) {
                    stop = true;
                } else {
                    if(notesFiltered.size() > minimalNumberOfNotes)
                        SimpleMidiImageCreator.createSimplePianoRollFromNotes(
                                notesFiltered, folder + "\\" + fileName + "_ch" + channel + "_" + counter++ + ".png",
                                notesFilteredStart, notesFilteredEnd - 1, scale);
                    if(nextNotesFiltered.size() > minimalNumberOfNotes)
                        SimpleMidiImageCreator.createSimplePianoRollFromNotes(
                                nextNotesFiltered, folder + "\\" + fileName + "_ch" + channel + "_" + counter++ + ".png",
                                nextStart, nextEnd - 1, scale);
                }

            } while(!stop);
        }
    }

}
