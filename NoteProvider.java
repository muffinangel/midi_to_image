import javafx.util.Pair;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteProvider {

    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final int INSTRUMENT_NAME = 0x04;
    public static final int MIDI_CHANNEL_PREFIX = 0x20;
    public static final int SET_TEMPO = 0x51;
    public static final int TIME_SIGNATURE = 0x58;

    private static int tempo = 120; // BMP, can be changed in set tempo events
    private static float PPQ = 480; // set once in midi header
    private static int resolution;
    private static float division;
    private static int microsecondPerQuaterNote = 500000;
    private static Map<Integer, String> instrumentPerChannel = new HashMap<>();
    private static Integer channelPrefixSet = null;

    public Map<Integer, List<NoteInformation>> getMidiNotes(String fileName) {

        Sequence sequence = null;
        try {
            sequence = MidiSystem.getSequence(new File(fileName));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.PPQ = sequence.PPQ;
        this.division = sequence.getDivisionType();
        this.resolution = sequence.getResolution();

        System.out.println("PPQ = " + this.PPQ);
        System.out.println("Division = " + this.division);
        System.out.println("Resolution = " + this.resolution);

//        if(sequence.getResolution() != 0) {
//            this.resolution = sequence.getResolution();
//        }

        Map<Integer, List<NoteInformation>> notesOn = new HashMap<>();
        Map<Integer, List<NoteInformation>> notesOff = new HashMap<>();

        int trackNumber = 0;
        for (Track track : sequence.getTracks()) {
            trackNumber++;
            System.out.println("Track " + trackNumber + ": size = " + track.size());
            System.out.println();
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                System.out.print("@" + event.getTick() + " ");
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    int channel = sm.getChannel();
                    System.out.print("Channel: " + channel + " ");
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        int velocity = sm.getData2();
                        long time = event.getTick();
                        //System.out.println("Note on, " + octave + " key=" + key + " velocity: " + velocity + " tick: " + time);
                        addNoteToMap(key, velocity, time, channel, true, notesOn);
                    } else if (sm.getCommand() == NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        int velocity = sm.getData2();
                        long time = event.getTick();
                        //System.out.println("Note off, " + octave + " key=" + key + " velocity: " + velocity);
                        addNoteToMap(key, velocity, time, channel, false, notesOff);
                    } else {
                        System.out.println("Command:" + sm.getCommand());
                    }
                } else {
                    // Set tempo event
                    if(message instanceof MetaMessage) {
                        if(((MetaMessage) message).getType() == SET_TEMPO) {
                            byte[] data = ((MetaMessage) message).getData();
                            int tempoinBMP = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                            this.tempo = 60000000 / tempoinBMP;
                            this.microsecondPerQuaterNote = tempoinBMP;
                        }
                        else if(((MetaMessage) message).getType() == INSTRUMENT_NAME) {
                            String name = ((MetaMessage) message).getData().toString();
                            Integer channel = getChannelFromName(name);
                            if(channel == null)
                                channel = channelPrefixSet;
                            instrumentPerChannel.put(channel, name);
                        }
                        else if(((MetaMessage) message).getType() == MIDI_CHANNEL_PREFIX) {
                            byte[] data = ((MetaMessage) message).getData();
                            byte channelNumber = data[0];
                            channelPrefixSet = (int) channelNumber;
                        }
                        else if(((MetaMessage) message).getType() == TIME_SIGNATURE) {
                            byte[] data = ((MetaMessage) message).getData();
                            int nominator = data[0];
                            int denominator = data[1];
                            int clockBetweenMetronomeClicks = data[2];
                            int notated32ndNotesInMIDIQuarterNote = data[3]; // should be 8

                            // according to: http://www.lastrayofhope.co.uk/2009/12/23/midi-delta-time-ticks-to-seconds/
                            // not sure if it should look like that
                            // http://www.somascape.org/midi/tech/mfile.html
                            double kTimeSignatureDenominator = Math.pow((double)2, (double)denominator);
                            double BPM = ( 60000000 / this.microsecondPerQuaterNote ) * ( kTimeSignatureDenominator / nominator ); // ?
                            this.microsecondPerQuaterNote = (int) (60000000 / BPM);
                            this.tempo = (int) BPM;
                        }

                    }
                    else if(event instanceof MidiEvent) {
                        // TODO PROGRAM CHANGE can change the instrument
                        // POLYPHONIC PRESSURE change the pressure if a note
                        // CHANNEL PRESSURE
                        // PITCH BEND
                    }
                    System.out.println("Other message: " + message.getClass());
                }
            }

            System.out.println();
        }

        for(var c : notesOn.keySet()) {
            System.out.println("Channel " + c + " -> " + notesOn.get(c).size());
        }
        System.out.println("OFF");

        for(var c : notesOff.keySet()) {
            System.out.println("Channel " + c + " -> " + notesOff.get(c).size());
        }

        return getMidiNotes(notesOn, notesOff);
    }

    public File getMidiFile(Map<Integer, List<NoteInformation>> notes, Map<Integer, String> instrumentPerChannel, String fileName) {
        try {
            Sequence sec = new Sequence(getDivisionType(notes), Math.round(getResolution(notes)));
            for(var channel : notes.keySet()) {
                Track t =  sec.createTrack();
                // TODO set the channel
                //****  General MIDI sysex -- turn on General MIDI sound set  ****
                byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
                SysexMessage sm = new SysexMessage();
                sm.setMessage(b, 6);
                MidiEvent me = new MidiEvent(sm,(long)0);
                t.add(me);
                //****  set tempo (meta event)  ****
                MetaMessage mt = new MetaMessage();
                byte[] bt = getTempo(notes.get(channel));
                mt.setMessage(0x51 ,bt, 3);
                me = new MidiEvent(mt,(long)0);
                t.add(me);
                // TODO is below instruction neccessary?
                //****  set omni on  ****
                ShortMessage mm = new ShortMessage();
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
                for(var n: notes.get(channel)) {
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

                    break;
                }

                //****  set end of track (meta event) 10 ticks later  ****
                mt = new MetaMessage();
                byte[] bet = {}; // empty array
                mt.setMessage(0x2F,bet,0);
                me = new MidiEvent(mt, maxEndValue + 10);
                t.add(me);
            }


            // write to the file
            File f = new File(fileName);
            MidiSystem.write(sec,1,f);
            return f;
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    private float getDivisionType(Map<Integer, List<NoteInformation>> notes) { // TODO
        // get division time from the channel that is the smallest integer and the first played note
        if(notes.isEmpty())
            return 0;
        var channels = notes.keySet();
        var noteFromChannel = notes.get(channels.toArray()[0]);
        return noteFromChannel.get(0).divisionTime;
    }

    private float getResolution(Map<Integer, List<NoteInformation>> notes) { // TODO
        // get resolution (tempo?)
        if(notes.isEmpty())
            return 24;
        var channels = notes.keySet();
        var noteFromChannel = notes.get(channels.toArray()[0]);
        return noteFromChannel.get(0).resolution;
    }

    private Integer getChannelFromName(String name) { // TODO
        return null;
    }

    private byte[] getTempo(List<NoteInformation> notes) { // TODO
        byte[] data =  {0x02, (byte)0x00, 0x00};
        if(notes.isEmpty())
            return data; // it doesn't matter

        int tempoFromNote = notes.get(0).tempo;
        int tempoinBMP =   60000000 / tempoFromNote;
        data[2] = (byte) tempoinBMP;
        data[1] = (byte) (tempoinBMP >>> 8);
        data[0] = (byte) (tempoinBMP >>> 8);

        //int tempoinBMP = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
        return data;
    }

    public Map<Integer, List<NoteInformation>> getMidiNotes(Map<Integer, List<NoteInformation>> on, Map<Integer, List<NoteInformation>> off) {
        Map<Integer, List<NoteInformation>> notes = new HashMap<>();

        for(var channel: on.keySet()) {
            var notesOn = on.get(channel);
            var notesOff = off.get(channel);

            if(notesOff == null || notesOff.isEmpty()) {
                notes.put(channel, notesOn);
                System.out.println("In channel " + channel + " there are only note on");
            }
            else {
                notes.put(channel, mergeNotesInformation(notesOn, notesOff));
            }
        }

        return notes;
    }

    private void addNoteToMap(int key, int velocity, long time, int channel, boolean isOn,  Map<Integer, List<NoteInformation>> notes) {
        var list = notes.get(channel);
        if(list == null) {
            notes.put(channel, new ArrayList<>());
        }
        list = notes.get(channel);
        Long startTime = null;
        Long endTime = null;
        Integer startVelocity = null;
        Integer endVelocity = null;
        if(isOn) {
            startTime = time;
            startVelocity = velocity;
        }
        else {
            endTime = time;
            endVelocity = velocity;
        }

        list.add(new NoteInformation(startTime, endTime, channel, key, startVelocity, endVelocity,
                this.PPQ, this.tempo, instrumentPerChannel.get(channel), division, resolution));
    }

    private NoteInformation mergeMatchingNotes(NoteInformation noteOn, List<NoteInformation> notesOff) {
        List<NoteInformation> matchingNotes = new ArrayList<>();
        for(var off : notesOff) {
            if(off.key == noteOn.key && off.endTime > noteOn.startTime) {
                    matchingNotes.add(off);
            }
        }

        if(matchingNotes.isEmpty())
            return noteOn; // never ending note

        NoteInformation bestNote = matchingNotes.get(0);
        long smallestDifference = bestNote.endTime - noteOn.startTime;

        for(var off: matchingNotes) {
            if(off.endTime - noteOn.startTime < smallestDifference) {
                bestNote = off;
                smallestDifference = off.endTime - noteOn.startTime;
            }
        }

        return new NoteInformation(noteOn.startTime, bestNote.endTime,
                noteOn.channel, noteOn.key, noteOn.velocity, bestNote.velocity,
                noteOn.PPQ, noteOn.tempo, noteOn.instrument, division, resolution);
    }

    private List<NoteInformation> mergeNotesInformation(List<NoteInformation> notesOn, List<NoteInformation> notesOff) {
        List<NoteInformation> merged = new ArrayList<>();
        int unmatchedNotesOnCounter = 0;
        for(var noteOn : notesOn) {
            var matched = mergeMatchingNotes(noteOn, notesOff);
            merged.add(matched);
            if(matched == noteOn)
                unmatchedNotesOnCounter++;
            else {
                var matchedNoteOff = new NoteInformation(null, matched.endTime,
                        matched.channel, matched.key, null, matched.velocityAtEnd,
                        matched.PPQ, matched.tempo, matched.instrument, matched.divisionTime, matched.resolution);
                notesOff.remove(matchedNoteOff);
            }
        }

        System.out.println("Channel " + notesOn.get(0).channel + "Not found a matching note for " + unmatchedNotesOnCounter + " notes");
        return merged;
    }

    /**
     *
     * @param notes - list of notes @NotNull
     * @param start - in milliseconds
     * @param end - in milliseconds
     * @param cutNotes - if beginning of note or end of it is outside of boundaries, then if this is set to true, the part of the note
     *                 that is inside time boundaries is added
     * @param noteTimeScale - how to scale time
     * @return - list of filtered notes within time boundaries @NotNull
     */
    public List<NoteInformation> filterNotes(List<NoteInformation> notes, long start, long end, Double noteTimeScale, boolean cutNotes) {
        List<NoteInformation> noteInformations = new ArrayList<>();
        System.out.println("Note size " + notes.size());
        double maxMidiOff = 0;
        for(var n: notes) {
            //System.out.println(n);
            System.out.println("# start = " + n.startTime + " : " + n.tickToMilliseconds(n.startTime) + "ms ; end = "
                    + n.endTime + " : " + n.tickToMilliseconds(n.endTime) + " ms");
            var noteStart = n.getStartTimeInMilliSeconds();
            var noteEnd = n.getEndTimeInMilliSeconds();
            if(noteEnd != null && noteStart != null) {
                noteStart = noteStart * noteTimeScale;
                noteEnd = noteEnd * noteTimeScale;
                if(maxMidiOff < noteEnd) {
                    maxMidiOff = noteEnd;
                }
                if (noteStart.intValue() >= start && noteEnd.intValue() <= end) {
                    noteInformations.add(n);
                } else if (cutNotes) {
                    if (noteStart.intValue() >= start && noteStart <= end) {
                        noteInformations.add(new NoteInformation(n.startTime, n.convertToTicks(Double.valueOf(end)),
                                n.channel, n.key, n.velocity, n.velocityAtEnd,
                                n.PPQ, n.tempo, n.instrument, n.divisionTime, n.resolution));
                    } else if (noteEnd.intValue() <= end && start <= noteEnd) {
                        noteInformations.add(new NoteInformation(n.convertToTicks(Double.valueOf(start)), n.endTime,
                                n.channel, n.key, n.velocity, n.velocityAtEnd,
                                n.PPQ, n.tempo, n.instrument, n.divisionTime, n.resolution));
                    }
                }
            }
            else if(cutNotes && noteStart != null) {
                // get this note from min(note start time, start param) to end param
                noteStart = noteStart * noteTimeScale;
                if(noteStart >= start && noteStart <= end)
                noteInformations.add(new NoteInformation(n.startTime, n.convertToTicks(Double.valueOf(end)),
                        n.channel, n.key, n.velocity, n.velocityAtEnd,
                        n.PPQ, n.tempo, n.instrument, n.divisionTime, n.resolution));
                else if(noteStart < start)
                    noteInformations.add(new NoteInformation( n.convertToTicks(Double.valueOf(start)),
                            n.convertToTicks(Double.valueOf(end)),
                            n.channel, n.key, n.velocity, n.velocityAtEnd,
                            n.PPQ, n.tempo, n.instrument, n.divisionTime, n.resolution));
            }
        }

        System.out.println("MAX = " + maxMidiOff);

        return noteInformations;
    }
}
