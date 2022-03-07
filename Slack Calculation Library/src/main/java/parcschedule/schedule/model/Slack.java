package parcschedule.schedule.model;

public class Slack {
    private int startTime;
    private int endTime;
    private String processor;
    private String fromTask;

    public Slack(int startTime, int endTime, String processor, String fromTask) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.processor = processor;
        this.fromTask = fromTask;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public String getFromTask() {
        return fromTask;
    }

    public int getAmount() {
        return this.endTime - this.startTime;
    }

    public String getProcessor() {
        return processor;
    }
}