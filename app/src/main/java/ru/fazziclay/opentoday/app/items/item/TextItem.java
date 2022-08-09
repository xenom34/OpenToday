package ru.fazziclay.opentoday.app.items.item;

import android.graphics.Color;

import org.json.JSONObject;

import ru.fazziclay.opentoday.annotation.Getter;
import ru.fazziclay.opentoday.annotation.JSONName;
import ru.fazziclay.opentoday.annotation.RequireSave;
import ru.fazziclay.opentoday.annotation.Setter;

public class TextItem extends Item {
    private static final String DEFAULT_TEXT_COLOR = "#ff0000ff";

    // START - Save
    public final static TextItemIETool IE_TOOL = new TextItemIETool();
    public static class TextItemIETool extends Item.ItemIETool {
        @Override
        public JSONObject exportItem(Item item) throws Exception {
            TextItem textItem = (TextItem) item;
            return super.exportItem(textItem)
                    .put("text", textItem.text)
                    .put("textColor", textItem.textColor)
                    .put("customTextColor", textItem.customTextColor)
                    .put("clickableUrls", textItem.clickableUrls);
        }

        private final TextItem defaultValues = new TextItem("<import_error>");
        @Override
        public Item importItem(JSONObject json) throws Exception {
            TextItem o = new TextItem(super.importItem(json), json.optString("text", defaultValues.text));
            o.textColor = json.optInt("textColor", defaultValues.textColor);
            o.customTextColor = json.optBoolean("customTextColor", defaultValues.customTextColor);
            o.clickableUrls = json.optBoolean("clickableUrls", defaultValues.clickableUrls);
            return o;
        }
    }
    // END - Save

    public static TextItem createEmpty() {
        return new TextItem("");
    }

    @JSONName(name = "text") @RequireSave private String text;
    @JSONName(name = "textColor") @RequireSave private int textColor = Color.parseColor(DEFAULT_TEXT_COLOR);
    @JSONName(name = "customTextColor") @RequireSave private boolean customTextColor = false;
    @JSONName(name = "clickableUrls") @RequireSave private boolean clickableUrls = false;

    public TextItem(String text) {
        this(null, text);
    }

    // Append
    public TextItem(Item item, String text) {
        super(item);
        this.text = text;
    }

    // Copy
    public TextItem(TextItem copy) {
        super(copy);
        this.text = copy.text;
        this.textColor = copy.textColor;
        this.customTextColor = copy.customTextColor;
        this.clickableUrls = copy.clickableUrls;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Getter public String getText() { return text; }
    @Setter public void setText(String v) { this.text = v; }
    @Getter public int getTextColor() { return textColor; }
    @Setter public void setTextColor(int v) { this.textColor = v; }
    @Getter public boolean isCustomTextColor() { return customTextColor; }
    @Setter public void setCustomTextColor(boolean v) { this.customTextColor = v; }
    @Getter public boolean isClickableUrls() { return clickableUrls; }
    @Setter public void setClickableUrls(boolean clickableUrls) { this.clickableUrls = clickableUrls; }
}