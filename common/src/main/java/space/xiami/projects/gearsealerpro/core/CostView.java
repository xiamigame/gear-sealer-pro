package space.xiami.projects.gearsealerpro.core;

/** Material amounts currently available versus the cost of one sealing operation. */
public record CostView(int haveGold, int needGold, int havePainite, int needPainite, int haveFocus, int needFocus) {

    public boolean goldEnough() {
        return this.haveGold >= this.needGold;
    }

    public boolean painiteEnough() {
        return this.havePainite >= this.needPainite;
    }

    public boolean focusEnough() {
        return this.haveFocus >= this.needFocus;
    }

    public boolean affordable() {
        return goldEnough() && painiteEnough() && focusEnough();
    }

    public String goldText() {
        return text(this.haveGold, this.needGold);
    }

    public String painiteText() {
        return text(this.havePainite, this.needPainite);
    }

    public String focusText() {
        return text(this.haveFocus, this.needFocus);
    }

    private static String text(int have, int need) {
        return have + "/" + need;
    }
}
