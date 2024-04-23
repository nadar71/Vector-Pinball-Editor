package com.dozingcatsoftware.vectorpinball.model;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloatList;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asInt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.FieldElementCollection;
import com.dozingcatsoftware.vectorpinball.elements.FlipperElement;

public class FieldLayout {

    static final String WIDTH_PROPERTY = "width";
    static final String HEIGHT_PROPERTY = "height";
    static final String DELEGATE_PROPERTY = "delegate";
    static final String TARGET_TIME_RATIO_PROPERTY = "targetTimeRatio";
    static final String GRAVITY_PROPERTY = "gravity";
    static final String NUM_BALLS_PROPERTY = "numballs";
    static final String BALL_RADIUS_PROPERTY = "ballradius";
    static final String BALL_COLOR_PROPERTY = "ballcolor";
    static final String SECONDARY_BALL_COLOR_PROPERTY = "secondaryBallColor";
    static final String LAUNCH_POSITION_PROPERTY = "launchPosition";
    static final String LAUNCH_VELOCITY_PROPERTY = "launchVelocity";
    static final String LAUNCH_RANDOM_VELOCITY_PROPERTY = "launchVelocityRandomDelta";
    static final String LAUNCH_DEAD_ZONE_PROPERTY = "launchDeadZone";

    static final String VARIABLES_PROPERTY = "variables";
    static final String ELEMENTS_PROPERTY = "elements";

    Random RAND = new Random();

    private FieldLayout() {}

    public static FieldLayout layoutForLevel(Map<String, Object> levelMap, World world) {
        FieldLayout layout = new FieldLayout();
        layout.initFromLevel(levelMap, world);
        return layout;
    }

    Map<String, Object> allParameters;
    FieldElementCollection fieldElements;
    float width;
    float height;
    float gravity;
    int numberOfBalls;
    float ballRadius;
    Color ballColor;
    Color secondaryBallColor;
    float targetTimeRatio;
    List<Float> launchPosition;
    List<Float> launchVelocity;
    List<Float> launchVelocityRandomDelta;
    List<Float> launchDeadZoneRect;

    static final Color DEFAULT_BALL_COLOR = Color.fromRGB(255, 0, 0);
    static final Color DEFAULT_SECONDARY_BALL_COLOR = Color.fromRGB(176, 176, 176);

    static List<?> listForKey(Map<?, ?> map, Object key) {
        if (map.containsKey(key)) return (List<?>) map.get(key);
        return Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    private FieldElementCollection createFieldElements(Map<String, Object> layoutMap, World world) {
        FieldElementCollection elements = new FieldElementCollection();

        Map<String, Object> variables = (Map<String, Object>) layoutMap.get(VARIABLES_PROPERTY);
        if (variables != null) {
            for (String varname : variables.keySet()) {
                elements.setVariable(varname, variables.get(varname));
            }
        }

        Set<Map<String, Object>> unresolvedElements = new HashSet<Map<String, Object>>();
        // Initial pass
        for (Object obj : listForKey(layoutMap, ELEMENTS_PROPERTY)) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> params = (Map<String, Object>) obj;
            try {
                elements.addElement(FieldElement.createFromParameters(params, elements, world));
            }
            catch (FieldElement.DependencyNotAvailableException ex) {
                unresolvedElements.add(params);
            }
        }

        return elements;
    }

    void initFromLevel(Map<String, Object> layoutMap, World world) {
        this.width = asFloat(layoutMap.get(WIDTH_PROPERTY), 20.0f);
        this.height = asFloat(layoutMap.get(HEIGHT_PROPERTY), 30.0f);
        this.gravity = asFloat(layoutMap.get(GRAVITY_PROPERTY), 4.0f);
        this.targetTimeRatio = asFloat(layoutMap.get(TARGET_TIME_RATIO_PROPERTY));
        this.numberOfBalls = asInt(layoutMap.get(NUM_BALLS_PROPERTY), 3);
        this.ballRadius = asFloat(layoutMap.get(BALL_RADIUS_PROPERTY), 0.5f);
        this.ballColor = colorFromMap(layoutMap, BALL_COLOR_PROPERTY, DEFAULT_BALL_COLOR);
        this.secondaryBallColor = colorFromMap(
                layoutMap, SECONDARY_BALL_COLOR_PROPERTY, DEFAULT_SECONDARY_BALL_COLOR);
        this.launchPosition = asFloatList(listForKey(layoutMap, LAUNCH_POSITION_PROPERTY));
        this.launchVelocity = asFloatList(listForKey(layoutMap, LAUNCH_VELOCITY_PROPERTY));
        this.launchVelocityRandomDelta = asFloatList(listForKey(layoutMap, LAUNCH_RANDOM_VELOCITY_PROPERTY));
        this.launchDeadZoneRect = asFloatList(listForKey(layoutMap, LAUNCH_DEAD_ZONE_PROPERTY));

        this.allParameters = layoutMap;
        this.fieldElements = createFieldElements(layoutMap, world);
    }

    private Color colorFromMap(Map<String, ?> map, String key, Color defaultColor) {
        @SuppressWarnings("unchecked")
        List<Number> value = (List<Number>) map.get(key);
        return (value != null) ? Color.fromList(value) : defaultColor;
    }

    public List<FieldElement> getFieldElements() {
        return fieldElements.getAllElements();
    }

    public List<FlipperElement> getFlipperElements() {
        return fieldElements.getFlipperElements();
    }
    public List<FlipperElement> getLeftFlipperElements() {
        return fieldElements.getLeftFlipperElements();
    }
    public List<FlipperElement> getRightFlipperElements() {
        return fieldElements.getRightFlipperElements();
    }

    public float getBallRadius() {
        return ballRadius;
    }

    public Color getBallColor() {
        return ballColor;
    }

    public Color getSecondaryBallColor() {
        return secondaryBallColor;
    }

    public int getNumberOfBalls() {
        return numberOfBalls;
    }

    public List<Float> getLaunchPosition() {
        return launchPosition;
    }

    public List<Float> getLaunchDeadZone() {
        return launchDeadZoneRect;
    }

    // Can apply random velocity increment if specified by "launchVelocityRandomDelta" key.
    public List<Float> getLaunchVelocity() {
        float vx = launchVelocity.get(0).floatValue();
        float vy = launchVelocity.get(1).floatValue();

        if (launchVelocityRandomDelta.size() >= 2) {
            if (launchVelocityRandomDelta.get(0) > 0) {
                vx += launchVelocityRandomDelta.get(0) * RAND.nextFloat();
            }
            if (launchVelocityRandomDelta.get(1) > 0) {
                vy += launchVelocityRandomDelta.get(1) * RAND.nextFloat();
            }
        }
        return Arrays.asList(vx, vy);
    }

    public float getWidth() {
        return width;
    }
    public float getHeight() {
        return height;
    }

    /**
     * Returns the desired ratio between real world time and simulation time. The application
     * should adjust the frame rate and/or time interval passed to Field.tick() to keep the
     * ratio as close to this value as possible.
     */
    public float getTargetTimeRatio() {
        return targetTimeRatio;
    }

    /** Returns the magnitude of the gravity vector. */
    public float getGravity() {
        return asFloat(allParameters.get("gravity"), 4.0f);
    }

    public String getDelegateClassName() {
        return (String)allParameters.get("delegate");
    }

    public String getScriptText() {
        return (String)allParameters.get("script");
    }

    public Object getValueWithKey(String key) {
        return fieldElements.getVariable(key);
    }
}
