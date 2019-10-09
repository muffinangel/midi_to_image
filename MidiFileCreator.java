import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MidiFileCreator {

    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final int INSTRUMENT_NAME = 0x04;


    public static File getMidiFile(Map<Integer, List<NoteInformation>> notes, Map<Integer, String> instrumentPerChannel, String fileName) {
        try {
            Sequence sec = new Sequence(getDivisionType(notes), Math.round(getResolution(notes)));
            for(var channel : notes.keySet()) {
                Track t =  sec.createTrack();
                ShortMessage mm;
                MidiEvent me;
                // TODO set the channel
                //****  General MIDI sysex -- turn on General MIDI sound set  ****
                byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
                SysexMessage sm = new SysexMessage();
                sm.setMessage(b, 6);
                me = new MidiEvent(sm,(long)0);
                t.add(me);
                //****  set tempo (meta event)  ****
                MetaMessage mt = new MetaMessage();
                byte[] bt = getTempo(notes.get(channel));
                //byte[] bt = {-5, -72, -40}; //(same as read)
                mt.setMessage(0x51 ,bt, 3);
                me = new MidiEvent(mt,(long)0);
                t.add(me);
                // TODO delete it
//                mt = new MetaMessage(); // added time signature (same as read0
//                byte[] sig = {6, 3, 18, 8};
//                mt.setMessage(0x58 , sig, 4);
//                me = new MidiEvent(mt,(long)0);
//                t.add(me);
//                mt = new MetaMessage(); // key sign
//                byte[] key = {-3, 0};
//                mt.setMessage(0x59 , key, 2);
//                me = new MidiEvent(mt,(long)0);
//                t.add(me);
                // end of deleting


                // TODO is below instruction neccessary?
                //****  set omni on  ****
                mm = new ShortMessage();
                mm.setMessage(0xB0, 0x7D,0x00);
                me = new MidiEvent(mm,(long)0);
                t.add(me);
                //****  set poly on  ****
                mm = new ShortMessage();
                mm.setMessage(0xB0, 0x7F,0x00);
                me = new MidiEvent(mm,(long)0);
                t.add(me);
                // TODO change it
                //****  set instrument to Piano  ****
                String piano = "piano";
                mt = new MetaMessage();
                mt.setMessage(INSTRUMENT_NAME, piano.getBytes(), piano.length());
                me = new MidiEvent(mt,(long)0);
                t.add(me);

                long maxEndValue = 0;
                List<NoteInformation> noteInformationList = notes.get(channel);
                for(var n: noteInformationList) {
                    if(n.velocityAtEnd == null)
                        n.velocityAtEnd = n.velocity;

                    mm = new ShortMessage();
                    mm.setMessage(NOTE_ON,n.key,n.velocity);
                    me = new MidiEvent(mm,n.startTime);
                    t.add(me);

                    mm = new ShortMessage();
                    mm.setMessage(NOTE_OFF,n.key,n.velocityAtEnd);
                    me = new MidiEvent(mm,n.endTime);
                    t.add(me);

                    if(n.endTime > maxEndValue)
                        maxEndValue = n.endTime;

                    //break;
                }

                //****  set end of track (meta event) 10 ticks later  ****
                mt = new MetaMessage();
                byte[] bet = {}; // empty array
                mt.setMessage(0x2F,bet,0);
                me = new MidiEvent(mt, maxEndValue + 100);
                t.add(me);
            }


            // write to the file
            File f = new File(fileName);
            MidiSystem.write(sec,0,f);
            return f;
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    private static float getDivisionType(Map<Integer, List<NoteInformation>> notes) { // TODO
        // get division time from the channel that is the smallest integer and the first played note
        if(notes.isEmpty())
            return 0;
        var channels = notes.keySet();
        var noteFromChannel = notes.get(channels.toArray()[0]);
        return noteFromChannel.get(0).divisionTime;
    }

    private static float getResolution(Map<Integer, List<NoteInformation>> notes) { // TODO
        // get resolution (tempo?)
        if(notes.isEmpty())
            return 24;
        var channels = notes.keySet();
        var noteFromChannel = notes.get(channels.toArray()[0]);
        return noteFromChannel.get(0).resolution;
    }

    private static byte[] getTempo(List<NoteInformation> notes) { // TODO
        byte[] data =  {0x02, (byte)0x00, 0x00};
        if(notes.isEmpty())
            return data; // it doesn't matter

        int tempoFromNote = notes.get(0).tempo;
        int tempoinBMP =   60000000 / tempoFromNote;
        data[2] = (byte) (tempoinBMP & 0xff);
        data[1] = (byte) (tempoinBMP >>> 8);
        data[0] = (byte) (tempoinBMP >>> 16);

        //int tempoinBMP = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
        return data;
    }
}
