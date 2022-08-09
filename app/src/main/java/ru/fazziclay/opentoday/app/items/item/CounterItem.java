package ru.fazziclay.opentoday.app.items.item;

import org.json.JSONObject;

import ru.fazziclay.opentoday.annotation.Getter;
import ru.fazziclay.opentoday.annotation.JSONName;
import ru.fazziclay.opentoday.annotation.RequireSave;
import ru.fazziclay.opentoday.annotation.Setter;

public class CounterItem extends TextItem {
    // START - Save
    public final static CounterItemIETool IE_TOOL = new CounterItemIETool();
    public static class CounterItemIETool extends TextItem.TextItemIETool {
        @Override
        public JSONObject exportItem(Item item) throws Exception {
            CounterItem counterItem = (CounterItem) item;
            return super.exportItem(item)
                    .put("counter", counterItem.counter)
                    .put("step", counterItem.step);
        }

        private final CounterItem defaultValues = new CounterItem("<import_error>");
        @Override
        public Item importItem(JSONObject json) throws Exception {
            CounterItem counterItem = new CounterItem((TextItem) super.importItem(json));
            counterItem.counter = json.optDouble("counter", defaultValues.counter);
            counterItem.step = json.optDouble("step", defaultValues.step);
            return counterItem;
        }
    }
    // END - Save

    public static CounterItem createEmpty() {
        return new CounterItem("");
    }

    @JSONName(name = "counter") @RequireSave private double counter = 0;
    @JSONName(name = "step") @RequireSave private double step = 1;

    public CounterItem(String text) {
        super(text);
    }

    public CounterItem(TextItem textItem) {
        super(textItem);
    }

    public CounterItem(CounterItem copy) {
        super(copy);
        this.counter = copy.counter;
        this.step = copy.step;
    }

    public void up() {
        counter = counter + step;
        save();
        updateUi();
    }

    public void down() {
        counter = counter - step;
        save();
        updateUi();
    }

    @Getter
    public double getCounter() { return counter; }
    @Getter
    public double getStep() { return step; }
    @Setter public void setCounter(double counter) { this.counter = counter; }
    @Setter public void setStep(double step) { this.step = step; }
}