public class NoteInformation {
    String instrument;
    Long startTime;
    Long endTime;
    Integer channel;
    Integer key;
    Integer velocity;
    Integer velocityAtEnd;
    // for time conversion
    float PPQ;
    Integer tempo;

    public NoteInformation(Long startTime, Long endTime,
                           Integer channel, Integer key, Integer velocity, Integer endVelocity,
                          float PPQ, Integer tempo, String instrument) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.channel = channel;
        this.key = key;
        this.velocity = velocity;
        this.velocityAtEnd = endVelocity;
        this.PPQ = PPQ;
        this.tempo = tempo;
        this.instrument = instrument;
    }

    public Double getStartTimeInSeconds() {
        return convertToSeconds(startTime);
    }

    public Double getEndTimeInSeconds() {
        return convertToSeconds(endTime);
    }

    private Double convertToSeconds(Long tick) {
        if(tick == null) return null;
        double scale = (60000000 /tempo) * (PPQ / 1000000) / (1000000) ; // 1/10 s ?
        return tick*scale;
    }

    public Long convertToTicks(Double seconds) {
        if(seconds == null) return null;
        double scale = (60000000 /tempo) * (PPQ / 1000000) / (1000000) ;
        return Math.round(seconds/scale);
    }

    @Override
    public String toString() {
        return "Note: \n[key = " + key + "]\n[channel = " + channel +"]\n[velocity = " + velocity + " : " + velocityAtEnd +
                "]\n" +  "[instrument = " + instrument + "]" + "[tick = " + startTime + " : " + endTime + "]\n"
                +  "[time = " + getStartTimeInSeconds() + ":" + getEndTimeInSeconds() + "]\n"
                + "tempo = " + tempo + "; PPQ = " + PPQ;
    }
}
