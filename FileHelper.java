
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
                    divideFile(noteProvider, notes, scale, pathToFolder, name);
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

}
