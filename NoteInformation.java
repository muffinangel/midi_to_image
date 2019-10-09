import javax.sound.midi.Sequence;

public class NoteInformation {
    String instrument;
    Long startTime;
    Long endTime;
    Integer channel;
    Integer key;
    Integer velocity;
    Integer velocityAtEnd;
    // for time conversion
    float PPQ; // TODO delete PPQ
    float divisionTime;
    float resolution;
    Integer tempo;

    public NoteInformation(Long startTime, Long endTime,
                           Integer channel, Integer key, Integer velocity, Integer endVelocity,
                          float PPQ, Integer tempo, String instrument, float divisionTime, float resolution) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.channel = channel;
        this.key = key;
        this.velocity = velocity;
        this.velocityAtEnd = endVelocity;
        this.PPQ = PPQ;
        this.tempo = tempo;
        this.instrument = instrument;
        this.divisionTime = divisionTime;
        this.resolution = resolution;
    }

    public Double getStartTimeInMilliSeconds() {
        return tickToMilliseconds(startTime);
    }

    public Double getEndTimeInMilliSeconds() {
        return tickToMilliseconds(endTime);
    }

//    private Double convertToSeconds(Long tick) { // microseconds?
//        if(tick == null) return null;
//        double scale = ((PPQ) / (60000000 /tempo)); //(60000000 /tempo) * (PPQ / 1000000) / (1000000) ; // 1/10 s ?
//        return tick*scale;
//    }
//
//    public Long convertToTicks(Double seconds) { // microseconds?
//        if(seconds == null) return null;
//        double scale = ((PPQ) / (60000000 /tempo));
//        return Math.round(seconds/scale);
//    }

    public Long convertToTicks(Double milliSeconds) {
        if(milliSeconds == null) return null;
        if(divisionTime != Sequence.PPQ) {
            Double scale = 1000 / (tempo * (double) divisionTime * (double) resolution);
            return Math.round(milliSeconds/scale);
        }

        return Math.round((( milliSeconds) / tempo * resolution));
    }

    public Double tickToMilliseconds(Long tick) {
        if(tick == null) return (double) 0;
        if(divisionTime != Sequence.PPQ) {
            Double t = Double.valueOf(60000000 / tempo);
            Double scale = 1000 / (t * (double) divisionTime * (double) resolution);
            return tick*scale;
        }
        return (((double) tick) * tempo / resolution);
    }

//1 millisecond =
//            0.001 seconds

//    private long tick2millis(long tick) {
//        if (divisionType != Sequence.PPQ) {
//            double dMillis = ((((double) tick) * 1000) /
//                    (tempoFactor * ((double) divisionType) * ((double) resolution)));
//            return (long) dMillis;
//        }
//        return MidiUtils.ticks2microsec(tick,
//                currTempo * inverseTempoFactor,
//                resolution) / 1000;
//    }

//private long millis2tick(long millis) {
//    if (divisionType != Sequence.PPQ) {
//        double dTick = ((((double) millis) * tempoFactor)
//                * ((double) divisionType)
//                * ((double) resolution))
//                / ((double) 1000);
//        return (long) dTick;
//    }
//    return MidiUtils.microsec2ticks(millis * 1000,
//            currTempo * inverseTempoFactor,
//            resolution);
//}

    @Override
    public String toString() {
        return "Note: \n[key = " + key + "]\n[channel = " + channel +"]\n[velocity = " + velocity + " : " + velocityAtEnd +
                "]\n" +  "[instrument = " + instrument + "]" + "[tick = " + startTime + " : " + endTime + "]\n"
                +  "[time = " + getStartTimeInMilliSeconds() + ":" + getEndTimeInMilliSeconds() + "]\n"
                + "tempo = " + tempo + "; PPQ = " + PPQ;
    }
}
