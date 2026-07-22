package v.akfz.glaze.addictivelight.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import v.akfz.aslib.render.color.Color;
import v.akfz.aslib.render.gui.widget.api.render.RenderPart;
import v.akfz.aslib.render.gui.widget.impl.button.ButtonWidget;
import v.akfz.aslib.render.gui.widget.impl.button.CheckboxWidget;
import v.akfz.aslib.render.gui.widget.impl.picker.ColorPickerWidget;
import v.akfz.aslib.render.gui.widget.impl.text.TextArea;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.aslib.util.json.JsonData;
import v.akfz.aslib.util.json.JsonFile;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.light.LightType;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.gui.widget.BlenderSpinnerWidget;
import v.akfz.glaze.addictivelight.gui.widget.OptionSelectorWidget;
import v.akfz.glaze.addictivelight.gui.widget.ResizableScrollContainer;
import v.akfz.glaze.addictivelight.render.AddictiveLight;
import v.akfz.glaze.addictivelight.render.passes.denoiser.Denoiser;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class MainGui extends Screen {
    private ResizableScrollContainer leftPanel;
    private ResizableScrollContainer rightPanel;
    private int selectedTab = 0;

    private String selectedBlockKey = null;

    private float splitRatio = 0.35f;
    private boolean draggingSplit = false;

    private RenderPart blueButton;
    private RenderPart blueCheckbox;

    private int pendingBlockShadowSize;
    private int pendingBlockEntityShadowSize;
    private int pendingEntityShadowSize;
    private int pendingParticleShadowSize;

    public MainGui() {
        super(Component.literal("Glaze - настройки"));
        initRenderers();
        SettingsData s = DataManager.INSTANCE.getSettingsData();
        this.pendingBlockShadowSize = s.blockShadowSize;
        this.pendingBlockEntityShadowSize = s.blockEntityShadowSize;
        this.pendingEntityShadowSize = s.entityShadowSize;
        this.pendingParticleShadowSize = s.particleShadowSize;
    }

    private void initRenderers() {
        this.blueButton = (graphics, mouseX, mouseY, delta, x, y, width, height, extras) -> {
            String text = extras.getOrDefault("text", String.class, "");
            boolean enabled = extras.getOrDefault("enabled", Boolean.class, true);
            boolean pressed = extras.getOrDefault("pressed", Boolean.class, false);
            boolean hovered = extras.getOrDefault("hovered", Boolean.class, false);

            int bg = pressed ? 0xDD003366 : (hovered ? 0x99002244 : 0x66051122);
            int border = hovered ? 0xFF00A2FF : 0xAA0055AA;
            int textCol = enabled ? 0xFFFFFFFF : 0x88888888;

            graphics.fill(x, y, x + width, y + height, bg);
            graphics.renderOutline(x, y, width, height, border);
            graphics.drawString(font, text, x + (width - font.width(text)) / 2, y + (height - font.lineHeight) / 2, textCol, true);
        };

        this.blueCheckbox = (graphics, mouseX, mouseY, delta, x, y, width, height, extras) -> {
            String text = extras.getOrDefault("text", String.class, "");
            boolean checked = extras.getOrDefault("checked", Boolean.class, false);
            boolean hovered = extras.getOrDefault("hovered", Boolean.class, false);

            int boxSize = Math.min(12, height);
            int boxX = x;
            int boxY = y + (height - boxSize) / 2;

            int bg = 0x66051122;
            int border = hovered ? 0xFF00A2FF : 0xAA0055AA;

            graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, bg);
            graphics.renderOutline(boxX, boxY, boxSize, boxSize, border);

            if (checked) {
                graphics.fill(boxX + 2, boxY + 2, boxX + boxSize - 2, boxY + boxSize - 2, 0xFF00A2FF);
            }
            graphics.drawString(font, text, boxX + boxSize + 4, y + (height - font.lineHeight) / 2, 0xFFFFFFFF, true);
        };
    }

    private boolean isShadowSizeChanged() {
        SettingsData s = DataManager.INSTANCE.getSettingsData();
        return pendingBlockShadowSize != s.blockShadowSize ||
                pendingBlockEntityShadowSize != s.blockEntityShadowSize ||
                pendingEntityShadowSize != s.entityShadowSize ||
                pendingParticleShadowSize != s.particleShadowSize;
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;

        int leftW = (int) (w * splitRatio);
        int rightX = 20 + leftW;
        int rightW = w - rightX - 10;
        int py = 30;
        int pHeight = h - 45;

        int leftAlpha = (int) (15 + (splitRatio * 40));
        int rightAlpha = (int) (15 + ((1.0f - splitRatio) * 40));

        int leftBgColor = (leftAlpha << 24) | 0x000A1A;
        int rightBgColor = (rightAlpha << 24) | 0x000A1A;

        this.leftPanel = new ResizableScrollContainer(10, py, leftW, pHeight);
        this.leftPanel.setContentWidth(leftW);
        this.leftPanel.setBgColor(leftBgColor);
        this.leftPanel.setScrollbarColor(0xAA00A2FF);
        this.leftPanel.setScrollbarBgColor(0x33001122);
        this.addRenderableWidget(leftPanel);

        SettingsData s = DataManager.INSTANCE.getSettingsData();
        ColorPickerWidget blockColorPicker;

        if (selectedTab == 5 && selectedBlockKey != null) {
            SettingsData.BlockLightSettings l = s.customLightBlocks.get(selectedBlockKey);
            if (l != null) {
                int pickerH = 80;
                int pickerY = py + 5;

                Color blockColor = new Color(l.r, l.g, l.b);
                blockColorPicker = new ColorPickerWidget(rightX + 5, pickerY, rightW - 15, pickerH, blockColor, col -> {
                    l.r = (float) col.getRed();
                    l.g = (float) col.getGreen();
                    l.b = (float) col.getBlue();
                });
                blockColorPicker.setBgColor(0x66051122);
                blockColorPicker.setBorderColor(0xAA0055AA);
                blockColorPicker.setSliderBgColor(0x33001122);
                this.addRenderableWidget(blockColorPicker);

                int offset = pickerH + 10;
                this.rightPanel = new ResizableScrollContainer(rightX, py + offset, rightW, pHeight - offset);
            } else {
                this.rightPanel = new ResizableScrollContainer(rightX, py, rightW, pHeight);
            }
        } else {
            this.rightPanel = new ResizableScrollContainer(rightX, py, rightW, pHeight);
        }

        this.rightPanel.setContentWidth(rightW);
        this.rightPanel.setBgColor(rightBgColor);
        this.rightPanel.setScrollbarColor(0xAA00A2FF);
        this.rightPanel.setScrollbarBgColor(0x33001122);
        this.addRenderableWidget(rightPanel);

        rebuildLeftPanel();
        rebuildRightPanel();
    }

    private void rebuildLeftPanel() {
        leftPanel.clearWidgets();
        int curY = 5;
        int width = leftPanel.getWidth() - 15;
        SettingsData s = DataManager.INSTANCE.getSettingsData();

        leftPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Глобальные настройки") {{
            this.mainRenderer = blueButton;
            this.setClickFunc((b, m) -> {
                selectedTab = 0;
                selectedBlockKey = null;
                MainGui.this.clearWidgets();
                MainGui.this.init();
            });
        }});
        curY += 22;

        leftPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Кастомные блоки") {{
            this.mainRenderer = blueButton;
            this.setClickFunc((b, m) -> {
                selectedTab = 5;
                selectedBlockKey = null;
                MainGui.this.clearWidgets();
                MainGui.this.init();
            });
        }});
        curY += 22;

        leftPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Настройки фильтрации") {{
            this.mainRenderer = blueButton;
            this.setClickFunc((b, m) -> {
                selectedTab = 4;
                selectedBlockKey = null;
                MainGui.this.clearWidgets();
                MainGui.this.init();
            });
        }});
        curY += 22;

        if (s.isDevMode) {
            leftPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Настройки клавиш") {{
                this.mainRenderer = blueButton;
                this.setClickFunc((b, m) -> {
                    selectedTab = 1;
                    selectedBlockKey = null;
                    MainGui.this.clearWidgets();
                    MainGui.this.init();
                });
            }});
            curY += 22;

            leftPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Отладка (Debug)") {{
                this.mainRenderer = blueButton;
                this.setClickFunc((b, m) -> {
                    selectedTab = 2;
                    selectedBlockKey = null;
                    MainGui.this.clearWidgets();
                    MainGui.this.init();
                });
            }});
            curY += 22;
        }

        if (GlobalUtils.isClientHost()) {
            leftPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Настройки хоста") {{
                this.mainRenderer = blueButton;
                this.setClickFunc((b, m) -> {
                    selectedTab = 3;
                    selectedBlockKey = null;
                    MainGui.this.clearWidgets();
                    MainGui.this.init();
                });
            }});
            curY += 22;
        }

        leftPanel.setContentHeight(curY + 2);
    }

    private void rebuildRightPanel() {
        rightPanel.clearWidgets();
        int curY = 5;
        int width = rightPanel.getWidth() - 15;
        SettingsData s = DataManager.INSTANCE.getSettingsData();

        if (selectedTab == 1) {
            rightPanel.addWidget(new KeybindButtonWidget(5, curY, width, 18, "Открыть редактор", s.KEY_TO_OPEN_SETTINGS, v -> s.KEY_TO_OPEN_SETTINGS = v, blueButton));
            curY += 22;

            rightPanel.addWidget(new KeybindButtonWidget(5, curY, width, 18, "Создать POINT свет", s.KEY_TO_ADD_LIGHT_POINT, v -> s.KEY_TO_ADD_LIGHT_POINT = v, blueButton));
            curY += 22;

            rightPanel.addWidget(new KeybindButtonWidget(5, curY, width, 18, "Создать SPOT свет", s.KEY_TO_ADD_LIGHT_SPOT, v -> s.KEY_TO_ADD_LIGHT_SPOT = v, blueButton));
            curY += 22;

            rightPanel.addWidget(new KeybindButtonWidget(5, curY, width, 18, "Создать AREA RECT свет", s.KEY_TO_ADD_LIGHT_AREA_RECT, v -> s.KEY_TO_ADD_LIGHT_AREA_RECT = v, blueButton));
            curY += 22;

            rightPanel.addWidget(new KeybindButtonWidget(5, curY, width, 18, "Создать AREA SPHERE свет", s.KEY_TO_ADD_LIGHT_AREA_SPHERE, v -> s.KEY_TO_ADD_LIGHT_AREA_SPHERE = v, blueButton));
            curY += 22;
        } else if (selectedTab == 2) {
            rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Общая отладка (debug)") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(s.debug);
                this.setOnToggle(v -> s.debug = v);
            }});
            curY += 20;

            rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Отладка теней (debugShadows)") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(s.debugShadows);
                this.setOnToggle(v -> s.debugShadows = v);
            }});
            curY += 20;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0, 3, s.debugShadow, "Индекс буфера теней (0-3)", true, v -> s.debugShadow = v.intValue()));
            curY += 22;

            rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Отключить отладку света полностью") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(s.debugLight);
                this.setOnToggle(v -> s.debugLight = v);
            }});
            curY += 20;

            rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Инфо источников(текст + радиус)") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(s.debugLightInfo);
                this.setOnToggle(v -> s.debugLightInfo = v);
            }});
            curY += 20;

            rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Frustum/Матрицы") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(s.debugLightFrustum);
                this.setOnToggle(v -> s.debugLightFrustum = v);
            }});
            curY += 20;
        } else if (selectedTab == 3) {
            rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Разрешить всем менять свет") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(s.isAllAllowedToChangeLightSources);
                this.setOnToggle(v -> s.isAllAllowedToChangeLightSources = v);
            }});
            curY += 20;

            rightPanel.addWidget(new ButtonWidget(5, curY, width, 14, "Разрешенные игроки (через запятую):") {{
                this.enabled = false;
                this.mainRenderer = (g, mx, my, d, rx, ry, rw, rh, ex) -> g.drawString(font, text, rx, ry + 2, 0xFFA0A2FF, false);
            }});
            curY += 16;

            TextArea playersArea = new TextArea(5, curY, width, 100);
            playersArea.setPlaceholder("Пример: Player1, Player2...");
            playersArea.setText(String.join(", ", s.allowedPlayers));
            playersArea.setLineRenderer((g, line, rx, ry, idx, startIdx, defColor) -> {
                g.drawString(font, line, rx, ry, 0xFFFFFFFF, false);
                s.allowedPlayers.clear();
                for (String p : playersArea.getText().split(",")) {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty()) {
                        s.allowedPlayers.add(trimmed);
                    }
                }
            });
            rightPanel.addWidget(playersArea);
            curY += 105;
        } else if (selectedTab == 4) {
            int[] denoiserOptions = {0, 1, 2, 3};
            rightPanel.addWidget(new OptionSelectorWidget(5, curY, width, 18, "Активный фильтр", denoiserOptions, s.pickedDenoiser.ordinal(), blueButton, v -> {
                s.pickedDenoiser = Denoiser.values()[v];
                rebuildRightPanel();
            }) {
                @Override
                public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                    this.text = "Фильтр: " + s.pickedDenoiser.name();
                    super.render(graphics, mouseX, mouseY, delta);
                }
            });
            curY += 22;

            if (s.pickedDenoiser == Denoiser.A_TROUS) {
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, 2.0, s.atrousDepthThreshold, "Порог глубины (Depth Threshold)", false, v -> s.atrousDepthThreshold = v.floatValue()));
                curY += 22;
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 1.0, 128.0, s.atrousNormalThreshold, "Порог нормалей (Normal Threshold)", false, v -> s.atrousNormalThreshold = v.floatValue()));
                curY += 22;
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, 10.0, s.atrousLumaThreshold, "Порог светимости (Luma Threshold)", false, v -> s.atrousLumaThreshold = v.floatValue()));
                curY += 22;
            } else if (s.pickedDenoiser == Denoiser.TAA) {
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, 1.0, s.taaBlendFactor, "Смешивание кадров (Blend Factor)", false, v -> s.taaBlendFactor = v.floatValue()));
                curY += 22;
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.5, 5.0, s.taaVarianceScale, "Мягкость клампа TAA (Variance Scale)", false, v -> s.taaVarianceScale = v.floatValue()));
                curY += 22;
            } else if (s.pickedDenoiser == Denoiser.SVGF) {
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, 1.0, s.taaBlendFactor, "Смешивание кадров (Blend Factor)", false, v -> s.taaBlendFactor = v.floatValue()));
                curY += 22;
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.5, 5.0, s.taaVarianceScale, "Мягкость клампа SVGF (Variance Scale)", false, v -> s.taaVarianceScale = v.floatValue()));
                curY += 22;
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, 2.0, s.svgfDepthThreshold, "Порог глубины (Depth Threshold)", false, v -> s.svgfDepthThreshold = v.floatValue()));
                curY += 22;
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 1.0, 128.0, s.svgfNormalThreshold, "Порог нормалей (Normal)", false, v -> s.svgfNormalThreshold = v.floatValue()));
                curY += 22;
                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, 32.0, s.svgfLumaThreshold, "Порог яркости (Luma)", false, v -> s.svgfLumaThreshold = v.floatValue()));
                curY += 22;
            }
        } else if (selectedTab == 5) {
            if (selectedBlockKey != null) {
                SettingsData.BlockLightSettings l = s.customLightBlocks.get(selectedBlockKey);
                if (l == null) {
                    selectedBlockKey = null;
                    MainGui.this.clearWidgets();
                    MainGui.this.init();
                    return;
                }

                rightPanel.addWidget(new ButtonWidget(5, curY, width, 18, "<- Назад к списку блоков") {{
                    this.mainRenderer = blueButton;
                    this.setClickFunc((b, m) -> {
                        selectedBlockKey = null;
                        MainGui.this.clearWidgets();
                        MainGui.this.init();
                    });
                }});
                curY += 22;

                rightPanel.addWidget(new ButtonWidget(5, curY, width, 14, "Редактирование: " + selectedBlockKey) {{
                    this.enabled = false;
                    this.mainRenderer = (g, mx, my, d, rx, ry, rw, rh, ex) -> g.drawString(font, text, rx, ry + 2, 0xFFA0A2FF, false);
                }});
                curY += 16;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, 1000.0, l.intensity, "Интенсивность", false, v -> l.intensity = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, 128.0, l.radius, "Радиус свечения", false, v -> l.radius = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new OptionSelectorWidget(5, curY, width, 18, "Тип источника", new int[]{0, 1, 2, 3, 4}, l.type, blueButton, v -> {
                    l.type = v;
                    rebuildRightPanel();
                }) {
                    @Override
                    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                        this.text = "Тип: " + LightType.values()[l.type % LightType.values().length].name();
                        super.render(graphics, mouseX, mouseY, delta);
                    }
                });
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 5.0, l.linear, "Линейный спад света", false, v -> l.linear = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 5.0, l.quadratic, "Квадратичный спад света", false, v -> l.quadratic = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, 10.0, l.falloffExponent, "Резкость затухания (Экспонента)", false, v -> l.falloffExponent = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Тени от блока") {{
                    this.mainRenderer = blueCheckbox;
                    this.setChecked(l.shadowsEnabled);
                    this.setOnToggle(v -> l.shadowsEnabled = v);
                }});
                curY += 20;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 5.0, l.shadowSoftness, "Мягкость теней", false, v -> l.shadowSoftness = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 0.1, l.shadowBias, "Смещение тени (Bias)", false, v -> l.shadowBias = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Объемный свет (Volumetric)") {{
                    this.mainRenderer = blueCheckbox;
                    this.setChecked(l.volumetric);
                    this.setOnToggle(v -> l.volumetric = v);
                }});
                curY += 20;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 20.0, l.volumetricStrength, "Сила объема", false, v -> l.volumetricStrength = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, -0.99, 0.99, l.mieG, "Анизотропия фазы (Mie G)", false, v -> l.mieG = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 1.0, l.fogDensity, "Плотность тумана", false, v -> l.fogDensity = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 1.0, l.fogAbsorption, "Поглощение тумана", false, v -> l.fogAbsorption = v.floatValue()));
                curY += 22;

                rightPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Удалить настройку блока") {{
                    this.mainRenderer = blueButton;
                    this.setClickFunc((b, m) -> {
                        s.customLightBlocks.remove(selectedBlockKey);
                        selectedBlockKey = null;
                        MainGui.this.clearWidgets();
                        MainGui.this.init();
                    });
                }});
                curY += 22;
            } else {
                rightPanel.addWidget(new ButtonWidget(5, curY, width, 14, "Добавить настройку блока (ID):") {{
                    this.enabled = false;
                    this.mainRenderer = (g, mx, my, d, rx, ry, rw, rh, ex) -> g.drawString(font, text, rx, ry + 2, 0xFFA0A2FF, false);
                }});
                curY += 16;

                TextArea addBlockArea = new TextArea(5, curY, width, 40);
                addBlockArea.setPlaceholder("Например: minecraft:sea_lantern");
                rightPanel.addWidget(addBlockArea);
                curY += 45;

                rightPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Добавить блок") {{
                    this.mainRenderer = blueButton;
                    this.setClickFunc((b, m) -> {
                        String val = addBlockArea.getText().trim();
                        if (!val.isEmpty() && !s.customLightBlocks.containsKey(val)) {
                            SettingsData.BlockLightSettings defaultSettings = new SettingsData.BlockLightSettings();
                            defaultSettings.intensity = 40.0f;
                            defaultSettings.radius = 10.0f;
                            s.customLightBlocks.put(val, defaultSettings);
                            selectedBlockKey = val;
                            MainGui.this.clearWidgets();
                            MainGui.this.init();
                        }
                    });
                }});
                curY += 22;

                rightPanel.addWidget(new ButtonWidget(5, curY, width, 14, "Список настроенных блоков:") {{
                    this.enabled = false;
                    this.mainRenderer = (g, mx, my, d, rx, ry, rw, rh, ex) -> g.drawString(font, text, rx, ry + 2, 0xFFA0A2FF, false);
                }});
                curY += 16;

                for (String blockKey : s.customLightBlocks.keySet()) {
                    rightPanel.addWidget(new ButtonWidget(5, curY, width, 18, blockKey) {{
                        this.mainRenderer = blueButton;
                        this.setClickFunc((b, m) -> {
                            selectedBlockKey = blockKey;
                            MainGui.this.clearWidgets();
                            MainGui.this.init();
                        });
                    }});
                    curY += 22;
                }
            }
        } else {
            rightPanel.addWidget(new CheckboxWidget(5, curY, width, 16, "Режим разработчика") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(s.isDevMode);
                this.setOnToggle(v -> {
                    s.isDevMode = v;
                    if (!v) {
                        selectedTab = 0;
                    }
                    MainGui.this.clearWidgets();
                    MainGui.this.init();
                });
            }});
            curY += 20;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, 1.0, s.renderScale, "Разрешение рендера (Scale)", false, v -> s.renderScale = v.floatValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 4, 16, s.shadowSamples, "Качество мягких теней (сэмплы)", true, v -> s.shadowSamples = v.intValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 4, 32, s.volumetricSteps, "Шаги объемных лучей", true, v -> s.volumetricSteps = v.intValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 10.0, s.exposure, "Экспозиция", false, v -> s.exposure = v.floatValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 10.0, s.contrast, "Контраст", false, v -> s.contrast = v.floatValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 10.0, s.saturation, "Насыщенность", false, v -> s.saturation = v.floatValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 8, 128, s.materialXZRadius, "Радиус скан. материалов XZ", true, v -> s.materialXZRadius = v.intValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 8, 128, s.materialYRadius, "Радиус скан. материалов Y", true, v -> s.materialYRadius = v.intValue()));
            curY += 22;

            rightPanel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 1, 256, s.maxLights, "Макс. источников", true, v -> s.maxLights = v.intValue()));
            curY += 22;

            int[] sizeOptions = {16, 32, 64, 128, 256, 512, 1024, 2048};

            rightPanel.addWidget(new OptionSelectorWidget(5, curY, width, 18, "Разрешение теней от блоков", sizeOptions, pendingBlockShadowSize, blueButton, v -> {
                pendingBlockShadowSize = v;
                rebuildRightPanel();
            }));
            curY += 22;

            rightPanel.addWidget(new OptionSelectorWidget(5, curY, width, 18, "Разрешение теней от блок-энтити", sizeOptions, pendingBlockEntityShadowSize, blueButton, v -> {
                pendingBlockEntityShadowSize = v;
                rebuildRightPanel();
            }));
            curY += 22;

            rightPanel.addWidget(new OptionSelectorWidget(5, curY, width, 18, "Разрешение теней от энтити", sizeOptions, pendingEntityShadowSize, blueButton, v -> {
                pendingEntityShadowSize = v;
                rebuildRightPanel();
            }));
            curY += 22;

            rightPanel.addWidget(new OptionSelectorWidget(5, curY, width, 18, "Разрешение теней от частиц", sizeOptions, pendingParticleShadowSize, blueButton, v -> {
                pendingParticleShadowSize = v;
                rebuildRightPanel();
            }));
            curY += 22;

            rightPanel.addWidget(new ButtonWidget(5, curY, width, 14, "Отключить ванильный свет у блоков (через запятую):") {{
                this.enabled = false;
                this.mainRenderer = (g, mx, my, d, rx, ry, rw, rh, ex) -> g.drawString(font, text, rx, ry + 2, 0xFFA0A2FF, false);
            }});
            curY += 16;

            TextArea disabledBlocksArea = new TextArea(5, curY, width, 60);
            disabledBlocksArea.setPlaceholder("Пример: minecraft:torch, minecraft:glowstone");
            disabledBlocksArea.setText(String.join(", ", s.disabledLightBlocks));
            disabledBlocksArea.setLineRenderer((g, line, rx, ry, idx, startIdx, defColor) -> {
                g.drawString(font, line, rx, ry, 0xFFFFFFFF, false);
                s.disabledLightBlocks.clear();
                for (String blockId : disabledBlocksArea.getText().split(",")) {
                    String trimmed = blockId.trim();
                    if (!trimmed.isEmpty()) {
                        s.disabledLightBlocks.add(trimmed);
                    }
                }
            });
            rightPanel.addWidget(disabledBlocksArea);
            curY += 65;

            if (isShadowSizeChanged()) {
                curY += 10;
                rightPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Изменения не применены!") {{
                    this.enabled = false;
                    this.mainRenderer = (graphics, mouseX, mouseY, delta, x, y, w, h, extras) -> {
                        graphics.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - font.lineHeight) / 2, 0xFFFF3333, true);
                    };
                }});
                curY += 22;

                rightPanel.addWidget(new ButtonWidget(5, curY, width, 18, "Применить изменения") {{
                    this.mainRenderer = blueButton;
                    this.setClickFunc((b, m) -> {
                        s.blockShadowSize = pendingBlockShadowSize;
                        s.blockEntityShadowSize = pendingBlockEntityShadowSize;
                        s.entityShadowSize = pendingEntityShadowSize;
                        s.particleShadowSize = pendingParticleShadowSize;
                        rebuildRightPanel();
                        AddictiveLight.INSTANCE.recreateShadowBuffers();
                    });
                }}.setTextTooltip(List.of("Внимание, из-за этого у вас может подвиснуть игра", "а изменение на большие значения требуют больше ресурсов")));
                curY += 22;
            }
        }

        rightPanel.setContentHeight(curY + 10);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!AddictiveLight.INSTANCE.isEnabled()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int splitX = 10 + (int) (this.width * splitRatio);
        if (button == 0 && mouseX >= splitX && mouseX < splitX + 10 && mouseY >= 30 && mouseY < this.height - 15) {
            this.draggingSplit = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingSplit) {
            this.draggingSplit = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!AddictiveLight.INSTANCE.isEnabled()) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        if (draggingSplit) {
            this.splitRatio = (float) Math.max(0.15, Math.min(0.85, mouseX / this.width));
            this.clearWidgets();
            this.init();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean isModuleActive = AddictiveLight.INSTANCE.isEnabled();

        if (this.leftPanel != null) this.leftPanel.setVisible(isModuleActive);
        if (this.rightPanel != null) this.rightPanel.setVisible(isModuleActive);

        if (!isModuleActive) {
            graphics.fill(0, 0, this.width, this.height, 0xEE000A1A);
            graphics.drawCenteredString(this.font, "МОДУЛЬ ОСВЕЩЕНИЯ ВЫКЛЮЧЕН", this.width / 2, this.height / 2 - 20, 0xFFFF3333);
            graphics.drawCenteredString(this.font, "Пожалуйста, включите модуль LS-Light в менеджере модулей,", this.width / 2, this.height / 2, 0xCCFFFFFF);
            graphics.drawCenteredString(this.font, "чтобы получить доступ к настройкам графики.", this.width / 2, this.height / 2 + 12, 0xCCFFFFFF);
            super.render(graphics, mouseX, mouseY, delta);
            return;
        }

        graphics.fill(0, 0, this.width, this.height, 0x40000000);

        int splitX = 10 + (int) (this.width * splitRatio);
        int handleColor = draggingSplit || (mouseX >= splitX && mouseX < splitX + 10 && mouseY >= 30 && mouseY < this.height - 15) ? 0xFF00A2FF : 0xAA0055AA;
        graphics.fill(splitX + 3, 30, splitX + 7, this.height - 15, handleColor);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFF00A2FF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        super.onClose();
        Path setPath = GlobalUtils.getAsLibCFGPath().resolve("glaze/light.json");
        GsonHelper.write(new JsonFile<>() {
            @Override
            public JsonData data() {
                return DataManager.INSTANCE.getSettingsData();
            }

            @Override
            public Path getPath() {
                return setPath;
            }
        });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static class KeybindButtonWidget extends ButtonWidget {
        private final String actionName;
        private final Consumer<Integer> onKeySet;
        private boolean listening = false;

        public KeybindButtonWidget(int x, int y, int width, int height, String actionName, int currentKey, Consumer<Integer> onKeySet, RenderPart renderer) {
            super(x, y, width, height, actionName + ": " + getKeyName(currentKey));
            this.actionName = actionName;
            this.onKeySet = onKeySet;
            this.mainRenderer = renderer;

            this.setClickFunc((btn, m) -> {
                if (!listening) {
                    listening = true;
                    this.text = "> Нажмите клавишу <";
                }
            });
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (listening) {
                onKeySet.accept(keyCode);
                listening = false;
                this.text = actionName + ": " + getKeyName(keyCode);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private static String getKeyName(int keyCode) {
            String name = GLFW.glfwGetKeyName(keyCode, 0);
            if (name != null) return name.toUpperCase();
            return switch (keyCode) {
                case 290 -> "F1";
                case 291 -> "F2";
                case 292 -> "F3";
                case 293 -> "F4";
                case 294 -> "F5";
                case 295 -> "F6";
                case 296 -> "F7";
                case 297 -> "F8";
                case 298 -> "F9";
                case 299 -> "F10";
                case 300 -> "F11";
                case 301 -> "F12";
                case 256 -> "ESCAPE";
                case 257 -> "ENTER";
                case 258 -> "TAB";
                case 259 -> "BACKSPACE";
                case 260 -> "INSERT";
                case 261 -> "DELETE";
                case 262 -> "RIGHT";
                case 263 -> "LEFT";
                case 264 -> "DOWN";
                case 265 -> "UP";
                case 340 -> "LSHIFT";
                case 341 -> "LCTRL";
                case 342 -> "LALT";
                case 344 -> "RSHIFT";
                case 345 -> "RCTRL";
                case 346 -> "RALT";
                default -> "KEY_" + keyCode;
            };
        }
    }
}