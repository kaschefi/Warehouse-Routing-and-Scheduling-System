package com.warehouse.ui;

import com.warehouse.model.domain.Node;
import com.warehouse.model.domain.NodeType;
import com.warehouse.model.domain.Robot;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import com.warehouse.service.RoutingService;
import com.warehouse.service.RobotManagementService;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import org.fxyz3d.importers.Importer3D;
import org.fxyz3d.importers.Model3D;
import java.util.List;

/**
 * Represents the 20x20 warehouse grid.
 * Displays the physical space where routing (Dijkstra) and MST will be
 * visualized.
 */
public class WarehouseGridView extends GridPane {

    public enum PlacementMode {
        SELECT,
        OBSTACLE,
        CHARGING_STATION,
        DROP_ZONE,
        ROBOT
    }

    private static final int ROWS = 15;
    private static final int COLS = 15;
    private static final double CELL_SIZE = 50.0;

    private final StatusBarView statusBar;
    private final RoutingService routingService;
    private final RightSidebarView rightSidebar;
    private final RobotManagementService robotManagementService;

    private final NodeType[][] gridState = new NodeType[ROWS][COLS];
    private final StackPane[][] cellGrid = new StackPane[ROWS][COLS];
    private PlacementMode currentPlacementMode = PlacementMode.SELECT;

    private final java.util.Map<String, String> stationLabels = new java.util.HashMap<>();
    private int chargingStationCounter = 0;
    private int dropZoneCounter = 0;
    private javafx.animation.Timeline dijkstraTimeline;
    private javafx.animation.Timeline mstTimeline;
    private final java.util.Set<String> activePathCells = new java.util.HashSet<>();
    private final java.util.Map<String, List<Edge>> activeRobotPaths = new java.util.HashMap<>();
    private final java.util.Map<String, String> robotReservations = new java.util.HashMap<>();
    private Image originalRobotTexture = null;
    private final java.util.Map<String, Image> customizedTextureCache = new java.util.HashMap<>();

    public java.util.Map<String, String> getStationLabels() {
        return stationLabels;
    }

    public WarehouseGridView(StatusBarView statusBar,
            RoutingService routingService,
            RightSidebarView rightSidebar,
            RobotManagementService robotManagementService) {
        this.statusBar = statusBar;
        this.routingService = routingService;
        this.rightSidebar = rightSidebar;
        this.robotManagementService = robotManagementService;
        this.setAlignment(Pos.CENTER);
        this.setHgap(1);
        this.setVgap(1);
        this.getStyleClass().add("warehouse-grid-pane");

        // Add 3D AmbientLight to ensure the robot model is bright and visible
        javafx.scene.AmbientLight ambientLight = new javafx.scene.AmbientLight(Color.rgb(240, 240, 240));
        this.getChildren().add(ambientLight);

        // Initialize grid state matrix
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gridState[r][c] = NodeType.EMPTY;
            }
        }

        // Apply explicit absolute, non-resizable constraints to all columns and rows
        for (int c = 0; c < COLS; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setMinWidth(CELL_SIZE);
            cc.setPrefWidth(CELL_SIZE);
            cc.setMaxWidth(CELL_SIZE);
            this.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < ROWS; r++) {
            javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints();
            rc.setMinHeight(CELL_SIZE);
            rc.setPrefHeight(CELL_SIZE);
            rc.setMaxHeight(CELL_SIZE);
            this.getRowConstraints().add(rc);
        }

        initializeGrid();
    }

    private void initializeGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                StackPane cell = createCell(row, col);
                cellGrid[row][col] = cell;
                this.add(cell, col, row);
            }
        }
    }

    private void updateRobotTelemetryHUD(Robot robot, StackPane robotVisual) {
        if (robotVisual == null)
            return;
        Text hudText = (Text) robotVisual.lookup("#robot-hud-" + robot.getId());
        if (hudText != null) {
            String taskId = robot.getActiveTaskId();
            if (taskId == null || taskId.isEmpty() || taskId.equals("None")) {
                hudText.setText(String.format("%s: %.0f%%", robot.getId(), robot.getBatteryLevel()));
            } else {
                hudText.setText(String.format("%s: %.0f%% [%s]", robot.getId(), robot.getBatteryLevel(), taskId));
            }
        }
    }

    private StackPane createRobotVisual(Robot robot) {
        StackPane robotVisual = new StackPane();
        robotVisual.setId("robot-visual-" + robot.getId());

        // Group to contain the 3D element hierarchy
        javafx.scene.Group agvGroup = new javafx.scene.Group();

        try {
            Color baseColor = getRobotPathColor(robot.getId());
            Color solidColor = Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 1.0);

            // Load the external 3D OBJ drone model
            java.net.URL modelUrl = getClass().getResource("/smart_drone.obj");
            Model3D model = Importer3D.load(modelUrl);
            javafx.scene.Group modelGroup = model.getRoot();

            // Load and apply texture
            PhongMaterial droneMat = new PhongMaterial();
            try {
                if (originalRobotTexture == null) {
                    originalRobotTexture = new Image(getClass().getResourceAsStream("/Uv Final.jpg"));
                }
                Image texture = customizedTextureCache.get(robot.getId());
                if (texture == null) {
                    texture = customizeRobotTexture(originalRobotTexture, solidColor);
                    customizedTextureCache.put(robot.getId(), texture);
                }
                droneMat.setDiffuseMap(texture);
            } catch (Exception e) {
                droneMat.setDiffuseColor(solidColor);
                droneMat.setSpecularColor(Color.WHITE);
            }

            // Create a separate material for the glowing propulsion rings
            PhongMaterial ringMat = new PhongMaterial();
            Color ringColor = Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.5);
            ringMat.setDiffuseColor(ringColor);
            ringMat.setSpecularColor(Color.WHITE);

            // Recursively apply texture material and propulsion material to MeshViews
            applyMaterialsToModel(modelGroup, droneMat, ringMat);

            // Wrap the modelGroup in a centering scaleGroup
            javafx.scene.Group scaleGroup = new javafx.scene.Group(modelGroup);
            Bounds bounds = modelGroup.getLayoutBounds();
            double maxDim = Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
            double scale = 40.0 / (maxDim > 0.0 ? maxDim : 1.0);

            // Center the model's pivot point at (0, 0, 0)
            modelGroup.setTranslateX(-(bounds.getMinX() + bounds.getMaxX()) / 2.0);
            modelGroup.setTranslateY(-(bounds.getMinY() + bounds.getMaxY()) / 2.0);
            modelGroup.setTranslateZ(-(bounds.getMinZ() + bounds.getMaxZ()) / 2.0);

            // Apply scale
            scaleGroup.setScaleX(scale);
            scaleGroup.setScaleY(scale);
            scaleGroup.setScaleZ(scale);

            // Rotate scaleGroup 180 degrees around the X-axis to stand the model upright
            scaleGroup.getTransforms()
                    .add(new javafx.scene.transform.Rotate(180, javafx.scene.transform.Rotate.X_AXIS));

            agvGroup.getChildren().add(scaleGroup);

            // 1. Hover Engine Pulse on ringMat (animating diffuse and specular properties to simulate glow)
            javafx.animation.Timeline pulseTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                            new javafx.animation.KeyValue(ringMat.diffuseColorProperty(), Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.4)),
                            new javafx.animation.KeyValue(ringMat.specularColorProperty(), Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.4))),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(600),
                            new javafx.animation.KeyValue(ringMat.diffuseColorProperty(), Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 1.0)),
                            new javafx.animation.KeyValue(ringMat.specularColorProperty(), Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 1.0))));
            pulseTimeline.setAutoReverse(true);
            pulseTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            pulseTimeline.play();
            robotVisual.getProperties().put("pulseTimeline", pulseTimeline);

            // 2. Floating Bobbing Wave on agvGroup along Y axis (drift up and down by 3.0
            // pixels, i.e., -1.5 to 1.5)
            javafx.animation.TranslateTransition bobbing = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.millis(1200), agvGroup);
            bobbing.setFromY(-1.5);
            bobbing.setToY(1.5);
            bobbing.setCycleCount(javafx.animation.Animation.INDEFINITE);
            bobbing.setAutoReverse(true);
            bobbing.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            bobbing.play();
            robotVisual.getProperties().put("bobbingTimeline", bobbing);

            // 3. Floating Arms (Inertial Sway & Symmetrical Travel Tilt)
            javafx.scene.Node leftArm = findNodeById(modelGroup, "pCube18");
            javafx.scene.Node rightArm = findNodeById(modelGroup, "pCube15");

            if (leftArm == null || rightArm == null) {
                // Fallback: search dynamically by layout bounds along the Z-axis (left/right
                // symmetrical axis)
                for (javafx.scene.Node n : getAllMeshViews(modelGroup)) {
                    Bounds b = n.getLayoutBounds();
                    double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
                    if (n.getId() != null && n.getId().startsWith("pCube")) {
                        if (cz < -1.5) {
                            leftArm = n;
                        } else if (cz > 1.5) {
                            rightArm = n;
                        }
                    }
                }
            }

            final javafx.scene.transform.Rotate leftArmSway = new javafx.scene.transform.Rotate(0,
                    javafx.scene.transform.Rotate.X_AXIS);
            final javafx.scene.transform.Rotate leftArmTilt = new javafx.scene.transform.Rotate(0,
                    javafx.scene.transform.Rotate.Y_AXIS);

            final javafx.scene.transform.Rotate rightArmSway = new javafx.scene.transform.Rotate(0,
                    javafx.scene.transform.Rotate.X_AXIS);
            final javafx.scene.transform.Rotate rightArmTilt = new javafx.scene.transform.Rotate(0,
                    javafx.scene.transform.Rotate.Y_AXIS);

            if (leftArm != null) {
                Bounds b = leftArm.getLayoutBounds();
                double cx = (b.getMinX() + b.getMaxX()) / 2.0;
                double cy = (b.getMinY() + b.getMaxY()) / 2.0;
                double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
                leftArmSway.setPivotX(cx);
                leftArmSway.setPivotY(cy);
                leftArmSway.setPivotZ(cz);
                leftArmTilt.setPivotX(cx);
                leftArmTilt.setPivotY(cy);
                leftArmTilt.setPivotZ(cz);
                leftArm.getTransforms().addAll(leftArmSway, leftArmTilt);
            }

            if (rightArm != null) {
                Bounds b = rightArm.getLayoutBounds();
                double cx = (b.getMinX() + b.getMaxX()) / 2.0;
                double cy = (b.getMinY() + b.getMaxY()) / 2.0;
                double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
                rightArmSway.setPivotX(cx);
                rightArmSway.setPivotY(cy);
                rightArmSway.setPivotZ(cz);
                rightArmTilt.setPivotX(cx);
                rightArmTilt.setPivotY(cy);
                rightArmTilt.setPivotZ(cz);
                rightArm.getTransforms().addAll(rightArmSway, rightArmTilt);
            }

            // Idle breathing rotation sway
            javafx.animation.Timeline idleSway = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                            new javafx.animation.KeyValue(leftArmSway.angleProperty(), -2.0),
                            new javafx.animation.KeyValue(rightArmSway.angleProperty(), -2.0)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.0),
                            new javafx.animation.KeyValue(leftArmSway.angleProperty(), 2.0),
                            new javafx.animation.KeyValue(rightArmSway.angleProperty(), 2.0)));
            idleSway.setAutoReverse(true);
            idleSway.setCycleCount(javafx.animation.Animation.INDEFINITE);
            idleSway.play();

            // Y-axis travel rotation (facing direction of travel)
            final javafx.scene.transform.Rotate travelRotate = new javafx.scene.transform.Rotate(-30,
                    javafx.scene.transform.Rotate.Y_AXIS);
            scaleGroup.getTransforms().add(travelRotate);

            robotVisual.getProperties().put("idleSway", idleSway);
            robotVisual.getProperties().put("leftArmSway", leftArmSway);
            robotVisual.getProperties().put("leftArmTilt", leftArmTilt);
            robotVisual.getProperties().put("rightArmSway", rightArmSway);
            robotVisual.getProperties().put("rightArmTilt", rightArmTilt);
            robotVisual.getProperties().put("travelRotate", travelRotate);

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback placeholder Box in case the loading fails
            javafx.scene.shape.Box fallback = new javafx.scene.shape.Box(40, 9, 32);
            PhongMaterial fallbackMat = new PhongMaterial(Color.web("#FF5722"));
            fallback.setMaterial(fallbackMat);
            agvGroup.getChildren().add(fallback);
        }

        // Isometric angle transformation
        javafx.scene.transform.Rotate rx = new javafx.scene.transform.Rotate(25, javafx.scene.transform.Rotate.X_AXIS);
        javafx.scene.transform.Rotate ry = new javafx.scene.transform.Rotate(40, javafx.scene.transform.Rotate.Y_AXIS);
        agvGroup.getTransforms().addAll(rx, ry);

        // Floating Telemetry HUD
        Text hudText = new Text();
        hudText.setId("robot-hud-" + robot.getId());
        hudText.setFill(Color.web("#00FF66")); // Glowing matrix green
        hudText.setFont(Font.font("Courier New", FontWeight.BOLD, 9.0));
        hudText.setTranslateY(-38.0); // Float cleanly above the drone model

        javafx.scene.effect.DropShadow hudGlow = new javafx.scene.effect.DropShadow();
        hudGlow.setColor(Color.web("#00FF66"));
        hudGlow.setRadius(2.0);
        hudGlow.setSpread(0.2);
        hudText.setEffect(hudGlow);

        robotVisual.getChildren().addAll(agvGroup, hudText);
        updateRobotTelemetryHUD(robot, robotVisual);
        return robotVisual;
    }

    private void applyMaterialsToModel(javafx.scene.Node node, PhongMaterial droneMat, PhongMaterial ringMat) {
        if (node instanceof javafx.scene.shape.MeshView) {
            javafx.scene.shape.MeshView mesh = (javafx.scene.shape.MeshView) node;
            String id = mesh.getId();
            if (id != null && (id.equalsIgnoreCase("pTorus1") || id.equalsIgnoreCase("pTorus2")
                    || id.equalsIgnoreCase("pTorus3") || id.toLowerCase().contains("torus"))) {
                mesh.setMaterial(ringMat);
            } else {
                mesh.setMaterial(droneMat);
            }
        } else if (node instanceof javafx.scene.Group) {
            for (javafx.scene.Node child : ((javafx.scene.Group) node).getChildren()) {
                applyMaterialsToModel(child, droneMat, ringMat);
            }
        }
    }

    private Image customizeRobotTexture(Image originalImage, Color targetColor) {
        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();
        if (width <= 0 || height <= 0) {
            return originalImage;
        }

        javafx.scene.image.WritableImage resultImage = new javafx.scene.image.WritableImage(width, height);
        javafx.scene.image.PixelReader reader = originalImage.getPixelReader();
        javafx.scene.image.PixelWriter writer = resultImage.getPixelWriter();

        double targetHue = targetColor.getHue();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = reader.getColor(x, y);
                double h = c.getHue();
                double s = c.getSaturation();
                double b = c.getBrightness();

                // Detect green hue range (typically 75° to 165°)
                if (h >= 75.0 && h <= 165.0 && s > 0.20 && b > 0.15) {
                    c = Color.hsb(targetHue, s, b, c.getOpacity());
                }
                writer.setColor(x, y, c);
            }
        }
        return resultImage;
    }

    private javafx.scene.Node findNodeById(javafx.scene.Node node, String id) {
        if (id.equals(node.getId())) {
            return node;
        }
        if (node instanceof javafx.scene.Group) {
            for (javafx.scene.Node child : ((javafx.scene.Group) node).getChildren()) {
                javafx.scene.Node res = findNodeById(child, id);
                if (res != null)
                    return res;
            }
        }
        return null;
    }

    private java.util.List<javafx.scene.Node> getAllMeshViews(javafx.scene.Node root) {
        java.util.List<javafx.scene.Node> meshViews = new java.util.ArrayList<>();
        getAllMeshViewsHelper(root, meshViews);
        return meshViews;
    }

    private void getAllMeshViewsHelper(javafx.scene.Node node, java.util.List<javafx.scene.Node> list) {
        if (node instanceof javafx.scene.shape.MeshView) {
            list.add(node);
        } else if (node instanceof javafx.scene.Group) {
            for (javafx.scene.Node child : ((javafx.scene.Group) node).getChildren()) {
                getAllMeshViewsHelper(child, list);
            }
        }
    }

    private void stopRobotVisualAnimations(javafx.scene.Node robotVisual) {
        if (robotVisual == null)
            return;
        javafx.animation.Timeline idleSway = (javafx.animation.Timeline) robotVisual.getProperties().get("idleSway");
        if (idleSway != null)
            idleSway.stop();
        javafx.animation.Timeline pulseTimeline = (javafx.animation.Timeline) robotVisual.getProperties()
                .get("pulseTimeline");
        if (pulseTimeline != null)
            pulseTimeline.stop();
        javafx.animation.Animation bobbingTimeline = (javafx.animation.Animation) robotVisual.getProperties()
                .get("bobbingTimeline");
        if (bobbingTimeline != null)
            bobbingTimeline.stop();
    }

    private void setRobotMovingState(Robot robot, boolean moving) {
        StackPane robotVisual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
        if (robotVisual == null)
            return;

        javafx.animation.Timeline idleSway = (javafx.animation.Timeline) robotVisual.getProperties().get("idleSway");
        javafx.scene.transform.Rotate leftArmSway = (javafx.scene.transform.Rotate) robotVisual.getProperties()
                .get("leftArmSway");
        javafx.scene.transform.Rotate leftArmTilt = (javafx.scene.transform.Rotate) robotVisual.getProperties()
                .get("leftArmTilt");
        javafx.scene.transform.Rotate rightArmSway = (javafx.scene.transform.Rotate) robotVisual.getProperties()
                .get("rightArmSway");
        javafx.scene.transform.Rotate rightArmTilt = (javafx.scene.transform.Rotate) robotVisual.getProperties()
                .get("rightArmTilt");
        javafx.scene.transform.Rotate travelRotate = (javafx.scene.transform.Rotate) robotVisual.getProperties()
                .get("travelRotate");

        if (leftArmSway == null || leftArmTilt == null || rightArmSway == null || rightArmTilt == null)
            return;

        if (moving) {
            if (idleSway != null) {
                idleSway.stop();
            }
            // Symmetrical backward tilt (left arm rotates by 10.0, right arm by -10.0)
            javafx.animation.Timeline tiltTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(300),
                            new javafx.animation.KeyValue(leftArmTilt.angleProperty(), 10.0,
                                    javafx.animation.Interpolator.EASE_OUT),
                            new javafx.animation.KeyValue(rightArmTilt.angleProperty(), -10.0,
                                    javafx.animation.Interpolator.EASE_OUT)));
            tiltTimeline.play();
        } else {
            // Smoothly ease back to idle sway starting point (0) and resume idle sway
            javafx.animation.Timeline returnTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(300),
                            new javafx.animation.KeyValue(leftArmTilt.angleProperty(), 0.0,
                                    javafx.animation.Interpolator.EASE_IN),
                            new javafx.animation.KeyValue(rightArmTilt.angleProperty(), 0.0,
                                    javafx.animation.Interpolator.EASE_IN)));
            returnTimeline.setOnFinished(e -> {
                if (idleSway != null) {
                    idleSway.play();
                }
            });
            returnTimeline.play();

            // Smoothly rotate the robot Y-axis back to its baseline default angle (-30.0
            // degrees)
            if (travelRotate != null) {
                javafx.animation.Timeline homeRotTimeline = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.millis(300),
                                new javafx.animation.KeyValue(travelRotate.angleProperty(), -30.0,
                                        javafx.animation.Interpolator.EASE_BOTH)));
                homeRotTimeline.play();
            }
        }
    }

    private StackPane createCell(int row, int col) {
        StackPane cell = new StackPane();
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(CELL_SIZE, CELL_SIZE);
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
        rect.setFill(Color.web("#0B132B"));
        rect.setStroke(Color.web("#00FF66", 0.15));
        rect.setArcWidth(4);
        rect.setArcHeight(4);

        cell.getChildren().add(rect);

        // Click handler to update state or select node for pathfinding
        cell.setOnMouseClicked(e -> {
            handleCellClick(row, col, rect);
        });

        // Hover effect to highlight path planning choices nicely
        cell.setOnMouseEntered(e -> {
            String cellId = "N_" + col + "_" + row;
            if (activePathCells.contains(cellId)) {
                rect.setFill(Color.web("#00E5FF", 0.4));
                rect.setStroke(Color.web("#00FF66"));
                rect.setStrokeWidth(1.5);
            } else {
                rect.setFill(Color.web("#1C2541")); // Lighter obsidian hover
                rect.setStroke(Color.web("#00FF66")); // Bright neon green border
                rect.setStrokeWidth(1.5);
            }
        });

        cell.setOnMouseExited(e -> {
            String cellId = "N_" + col + "_" + row;
            if (activePathCells.contains(cellId)) {
                rect.setFill(Color.web("#00E5FF", 0.4));
                rect.setStroke(Color.web("#00E5FF", 0.4));
                rect.setStrokeWidth(1.5);
            } else {
                rect.setFill(getColorForNodeType(gridState[row][col]));
                if (gridState[row][col] == NodeType.CHARGING_STATION) {
                    rect.setStroke(Color.web("#FFB703"));
                    rect.setStrokeWidth(1.5);
                } else if (gridState[row][col] == NodeType.DROP_ZONE) {
                    rect.setStroke(Color.web("#00FF66"));
                    rect.setStrokeWidth(1.5);
                } else {
                    rect.setStroke(Color.web("#00FF66", 0.15));
                    rect.setStrokeWidth(1.0);
                }
            }
        });

        return cell;
    }

    private void handleCellClick(int row, int col, Rectangle rect) {
        String cellId = "N_" + col + "_" + row;

        if (currentPlacementMode == PlacementMode.ROBOT) {
            if (gridState[row][col] == NodeType.OBSTACLE) {
                statusBar.setStatus("Warning: Cannot place a robot on an obstacle block!");
                return;
            }

            // Build the graph database to fetch a valid node reference
            routingService.generateGraphFromGrid(gridState);
            Graph graph = routingService.getWarehouseMap();
            Node targetNode = null;
            for (Node n : graph.getNodes()) {
                if (n.getId().equals(cellId)) {
                    targetNode = n;
                    break;
                }
            }
            if (targetNode == null) {
                targetNode = new Node(cellId, col, row, gridState[row][col]);
            }

            Robot robot = robotManagementService.registerRobot(targetNode);

            // Render the robot visually inside the StackPane cell
            StackPane cell = cellGrid[row][col];
            for (javafx.scene.Node child : new java.util.ArrayList<>(cell.getChildren())) {
                if (child.getId() != null && child.getId().startsWith("robot-visual-")) {
                    stopRobotVisualAnimations(child);
                }
            }
            cell.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("robot-visual-"));
            cell.getChildren().add(createRobotVisual(robot));
            cell.toFront();

            statusBar.setStatus("Placed " + robot.getName() + " at " + cellId);

        } else if (currentPlacementMode != PlacementMode.SELECT) {
            NodeType targetType = NodeType.EMPTY;
            if (currentPlacementMode == PlacementMode.OBSTACLE) {
                targetType = NodeType.OBSTACLE;
            } else if (currentPlacementMode == PlacementMode.CHARGING_STATION) {
                targetType = NodeType.CHARGING_STATION;
            } else if (currentPlacementMode == PlacementMode.DROP_ZONE) {
                targetType = NodeType.DROP_ZONE;
            }

            StackPane cell = cellGrid[row][col];
            // Clear any direct Text children (old labels)
            cell.getChildren().removeIf(node -> node instanceof Text);
            stationLabels.remove(cellId);

            // Edit layout mode: toggle or update node type
            if (gridState[row][col] == targetType) {
                gridState[row][col] = NodeType.EMPTY;
                rect.setFill(Color.web("#0B132B"));
                rect.setStroke(Color.web("#00FF66", 0.15));
                rect.setStrokeWidth(1.0);
                statusBar.setStatus("Cell " + cellId + " cleared to EMPTY");
            } else {
                gridState[row][col] = targetType;
                rect.setFill(getColorForNodeType(targetType));
                statusBar.setStatus("Cell " + cellId + " set to " + targetType);

                if (targetType == NodeType.CHARGING_STATION) {
                    chargingStationCounter++;
                    String label = "CS" + chargingStationCounter;
                    stationLabels.put(cellId, label);
                    Text textLabel = new Text(label);
                    textLabel.setFill(Color.web("#FFB703")); // Glowing amber
                    textLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 12));

                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#FFB703"));
                    glow.setRadius(3.0);
                    glow.setSpread(0.2);
                    textLabel.setEffect(glow);

                    rect.setStroke(Color.web("#FFB703"));
                    rect.setStrokeWidth(1.5);
                    cell.getChildren().add(textLabel);

                    // If a robot is already sitting on this cell, recharge it immediately
                    for (Robot r : robotManagementService.getActiveFleet()) {
                        if (r.getCurrentNode() != null && r.getCurrentNode().getX() == col
                                && r.getCurrentNode().getY() == row) {
                            r.setBatteryLevel(100.0);
                            r.getCurrentNode().setNodeType(NodeType.CHARGING_STATION);
                            StackPane visual = (StackPane) this.lookup("#robot-visual-" + r.getId());
                            updateRobotTelemetryHUD(r, visual);
                        }
                    }
                } else if (targetType == NodeType.DROP_ZONE) {
                    dropZoneCounter++;
                    String label = "DZ" + dropZoneCounter;
                    stationLabels.put(cellId, label);
                    Text textLabel = new Text(label);
                    textLabel.setFill(Color.web("#00FF66")); // Glowing green
                    textLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 12));

                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#00FF66"));
                    glow.setRadius(3.0);
                    glow.setSpread(0.2);
                    textLabel.setEffect(glow);

                    rect.setStroke(Color.web("#00FF66"));
                    rect.setStrokeWidth(1.5);
                    cell.getChildren().add(textLabel);
                }
            }

            // If a cell becomes an obstacle, and it was previously selected for routing or
            // has a robot, clear it
            if (gridState[row][col] == NodeType.OBSTACLE) {
                for (javafx.scene.Node child : new java.util.ArrayList<>(cell.getChildren())) {
                    if (child.getId() != null && child.getId().startsWith("robot-visual-")) {
                        stopRobotVisualAnimations(child);
                    }
                }
                cell.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("robot-visual-"));

                if (rightSidebar != null) {
                    if (rightSidebar.getStartNodeText().equals(cellId)) {
                        rightSidebar.setStartNodeText("");
                    }
                    if (rightSidebar.getEndNodeText().equals(cellId)) {
                        rightSidebar.setEndNodeText("");
                    }
                }
            }
        } else {
            // Pathfinding Selection Mode
            if (gridState[row][col] == NodeType.OBSTACLE) {
                statusBar.setStatus("Warning: Cannot select an obstacle block (" + cellId + ") as a routing node!");
                return;
            }

            if (rightSidebar != null) {
                String start = rightSidebar.getStartNodeText();
                String end = rightSidebar.getEndNodeText();
                if (start.isEmpty()) {
                    rightSidebar.setStartNodeText(cellId);
                    statusBar.setStatus("Selected Start Node: " + cellId);
                } else if (end.isEmpty()) {
                    rightSidebar.setEndNodeText(cellId);
                    statusBar.setStatus("Selected End Node: " + cellId);
                } else {
                    rightSidebar.setStartNodeText(cellId);
                    rightSidebar.setEndNodeText("");
                    statusBar.setStatus("Selected Start Node: " + cellId + " (End Node cleared)");
                }
            } else {
                statusBar.setStatus("Cell selected: (" + col + "," + row + ")");
            }
        }
    }

    private Color getColorForNodeType(NodeType type) {
        if (type == null)
            return Color.web("#0B132B");
        switch (type) {
            case OBSTACLE:
                return Color.web("#FF3333"); // Crimson Warning Red
            case CHARGING_STATION:
            case DROP_ZONE:
            case EMPTY:
            default:
                return Color.web("#0B132B");
        }
    }

    private Color getRobotPathColor(String robotId) {
        if (robotId == null) {
            return Color.web("#00E5FF", 0.4);
        }
        String[] neonColors = {
            "#00E5FF", // Neon Cyan/Blue (First robot / Default)
            "#BD00FF", // Neon Violet/Purple
            "#FF007F", // Neon Pink/Magenta
            "#00FF66", // Neon Green
            "#FF6C00", // Neon Orange/Amber
            "#FFE600", // Neon Yellow
            "#7B2CBF"  // Neon Deep Purple
        };
        int index = 0;
        if (robotId.startsWith("R")) {
            try {
                int num = Integer.parseInt(robotId.substring(1));
                index = (num - 1) % neonColors.length;
                if (index < 0) index = 0;
            } catch (NumberFormatException e) {
                index = Math.abs(robotId.hashCode()) % neonColors.length;
            }
        } else if (robotId.equalsIgnoreCase("MANUAL")) {
            index = 0;
        } else {
            index = Math.abs(robotId.hashCode()) % neonColors.length;
        }
        return Color.web(neonColors[index], 0.4);
    }

    public void redrawActiveGridLayers() {
        // Clear highlights on cells
        activePathCells.clear();

        // Reset non-obstacle cells back to default colors/borders
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (gridState[r][c] != NodeType.OBSTACLE) {
                    StackPane cell = cellGrid[r][c];

                    // Remove any Line nodes (from MST highlighting)
                    cell.getChildren().removeIf(node -> node instanceof javafx.scene.shape.Line);

                    Rectangle rect = (Rectangle) cell.getChildren().get(0);
                    rect.setFill(getColorForNodeType(gridState[r][c]));

                    if (gridState[r][c] == NodeType.CHARGING_STATION) {
                        rect.setStroke(Color.web("#FFB703"));
                        rect.setStrokeWidth(1.5);
                    } else if (gridState[r][c] == NodeType.DROP_ZONE) {
                        rect.setStroke(Color.web("#00FF66"));
                        rect.setStrokeWidth(1.5);
                    } else {
                        rect.setStroke(Color.web("#00FF66", 0.15));
                        rect.setStrokeWidth(1.0);
                    }
                }
            }
        }

        // Add all active path cells to the set for hover logic
        for (List<Edge> path : activeRobotPaths.values()) {
            if (path == null || path.isEmpty())
                continue;
            activePathCells.add(path.get(0).getSource().getId());
            for (Edge edge : path) {
                activePathCells.add(edge.getDestination().getId());
            }
        }

        // Highlight cells for each path
        for (java.util.Map.Entry<String, List<Edge>> entry : activeRobotPaths.entrySet()) {
            String robotId = entry.getKey();
            List<Edge> path = entry.getValue();
            if (path == null || path.isEmpty())
                continue;

            Color pathColor = getRobotPathColor(robotId);

            Node startNode = path.get(0).getSource();
            highlightCell(startNode.getY(), startNode.getX(), pathColor, pathColor, 1.5);

            for (Edge edge : path) {
                Node destNode = edge.getDestination();
                highlightCell(destNode.getY(), destNode.getX(), pathColor, pathColor, 1.5);
            }
        }
    }

    public void highlightCalculatedRoute(List<Edge> shortestPath) {
        highlightCalculatedRoute("MANUAL", shortestPath);
    }

    public void highlightCalculatedRoute(String robotId, List<Edge> shortestPath) {
        if (dijkstraTimeline != null) {
            dijkstraTimeline.stop();
        }

        if (shortestPath == null || shortestPath.isEmpty()) {
            activeRobotPaths.remove(robotId);
        } else {
            activeRobotPaths.put(robotId, shortestPath);
        }
        redrawActiveGridLayers();
    }

    public void highlightMST(List<Edge> mstEdges) {
        // Reset highlights
        resetGridHighlights();

        if (mstTimeline != null) {
            mstTimeline.stop();
        }

        if (mstEdges == null || mstEdges.isEmpty()) {
            return;
        }

        mstTimeline = new javafx.animation.Timeline();
        double edgeDurationMs = 200.0;

        for (int i = 0; i < mstEdges.size(); i++) {
            Edge edge = mstEdges.get(i);
            Node src = edge.getSource();
            Node dest = edge.getDestination();

            int x1 = src.getX();
            int y1 = src.getY();
            int x2 = dest.getX();
            int y2 = dest.getY();

            int dx = x2 - x1;
            int dy = y2 - y1;

            if (y1 >= 0 && y1 < ROWS && x1 >= 0 && x1 < COLS &&
                    y2 >= 0 && y2 < ROWS && x2 >= 0 && x2 < COLS) {

                StackPane cell1 = cellGrid[y1][x1];
                StackPane cell2 = cellGrid[y2][x2];

                javafx.scene.shape.Line line1 = new javafx.scene.shape.Line();
                line1.setManaged(false);
                line1.setStartX(CELL_SIZE / 2.0);
                line1.setStartY(CELL_SIZE / 2.0);
                line1.setEndX(CELL_SIZE / 2.0);
                line1.setEndY(CELL_SIZE / 2.0);
                line1.setStroke(Color.web("#39FF14")); // Matrix Laser Green
                line1.setStrokeWidth(2.0);

                javafx.scene.shape.Line line2 = new javafx.scene.shape.Line();
                line2.setManaged(false);
                line2.setStartX(CELL_SIZE / 2.0);
                line2.setStartY(CELL_SIZE / 2.0);
                line2.setEndX(CELL_SIZE / 2.0);
                line2.setEndY(CELL_SIZE / 2.0);
                line2.setStroke(Color.web("#39FF14")); // Matrix Laser Green
                line2.setStrokeWidth(2.0);

                javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                glow.setColor(Color.web("#39FF14"));
                glow.setRadius(5.0);
                glow.setSpread(0.6);
                line1.setEffect(glow);
                line2.setEffect(glow);

                cell1.getChildren().add(line1);
                cell2.getChildren().add(line2);

                double startTime = i * edgeDurationMs;
                double endTime = (i + 1) * edgeDurationMs;

                mstTimeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(startTime),
                        new javafx.animation.KeyValue(line1.endXProperty(), CELL_SIZE / 2.0),
                        new javafx.animation.KeyValue(line1.endYProperty(), CELL_SIZE / 2.0),
                        new javafx.animation.KeyValue(line2.endXProperty(), CELL_SIZE / 2.0),
                        new javafx.animation.KeyValue(line2.endYProperty(), CELL_SIZE / 2.0)));

                mstTimeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(endTime),
                        new javafx.animation.KeyValue(line1.endXProperty(), CELL_SIZE / 2.0 + dx * (CELL_SIZE / 2.0)),
                        new javafx.animation.KeyValue(line1.endYProperty(), CELL_SIZE / 2.0 + dy * (CELL_SIZE / 2.0)),
                        new javafx.animation.KeyValue(line2.endXProperty(), CELL_SIZE / 2.0 - dx * (CELL_SIZE / 2.0)),
                        new javafx.animation.KeyValue(line2.endYProperty(), CELL_SIZE / 2.0 - dy * (CELL_SIZE / 2.0))));
            }
        }
        mstTimeline.play();
    }

    private void highlightCell(int r, int c, Color fill, Color border, double borderWidth) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
            if (gridState[r][c] != NodeType.OBSTACLE) {
                StackPane cell = cellGrid[r][c];
                Rectangle rect = (Rectangle) cell.getChildren().get(0);
                rect.setFill(fill);
                rect.setStroke(border);
                rect.setStrokeWidth(borderWidth);
            }
        }
    }

    private void resetGridHighlights() {
        if (dijkstraTimeline != null) {
            dijkstraTimeline.stop();
        }
        activePathCells.clear();

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (gridState[r][c] != NodeType.OBSTACLE) {
                    StackPane cell = cellGrid[r][c];

                    // Remove any Line nodes (from MST highlighting)
                    cell.getChildren().removeIf(node -> node instanceof javafx.scene.shape.Line);

                    Rectangle rect = (Rectangle) cell.getChildren().get(0);
                    rect.setFill(getColorForNodeType(gridState[r][c]));

                    // Restore borders and styles for stations
                    if (gridState[r][c] == NodeType.CHARGING_STATION) {
                        rect.setStroke(Color.web("#FFB703"));
                        rect.setStrokeWidth(1.5);
                    } else if (gridState[r][c] == NodeType.DROP_ZONE) {
                        rect.setStroke(Color.web("#00FF66"));
                        rect.setStrokeWidth(1.5);
                    } else {
                        rect.setStroke(Color.web("#00FF66", 0.15));
                        rect.setStrokeWidth(1.0);
                    }
                }
            }
        }
    }

    public void clearMap() {
        if (dijkstraTimeline != null) {
            dijkstraTimeline.stop();
        }
        if (mstTimeline != null) {
            mstTimeline.stop();
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gridState[r][c] = NodeType.EMPTY;
            }
        }

        stationLabels.clear();
        chargingStationCounter = 0;
        dropZoneCounter = 0;
        activeRobotPaths.clear();
        activePathCells.clear();
        robotReservations.clear();
        customizedTextureCache.clear();

        if (rightSidebar != null) {
            rightSidebar.clearCustomTasks();
        }

        // Remove robot visual assets and reset cell colors/borders
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                StackPane cell = cellGrid[r][c];
                for (javafx.scene.Node child : new java.util.ArrayList<>(cell.getChildren())) {
                    if (child.getId() != null && child.getId().startsWith("robot-visual-")) {
                        stopRobotVisualAnimations(child);
                    }
                }
                // Keep only the first child (base Rectangle)
                while (cell.getChildren().size() > 1) {
                    cell.getChildren().remove(1);
                }
                Rectangle rect = (Rectangle) cell.getChildren().get(0);
                rect.setFill(Color.web("#0B132B"));
                rect.setStroke(Color.web("#00FF66", 0.15));
                rect.setStrokeWidth(1.0);
            }
        }

        statusBar.setStatus("Map cleared.");
    }

    public void placeStationProgrammatic(int col, int row, NodeType type) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        String cellId = "N_" + col + "_" + row;
        StackPane cell = cellGrid[row][col];
        cell.getChildren().removeIf(node -> node instanceof Text);
        stationLabels.remove(cellId);
        gridState[row][col] = type;
        Rectangle rect = (Rectangle) cell.getChildren().get(0);

        if (type == NodeType.CHARGING_STATION) {
            chargingStationCounter++;
            String label = "CS" + chargingStationCounter;
            stationLabels.put(cellId, label);
            Text textLabel = new Text(label);
            textLabel.setFill(Color.web("#FFB703")); // Glowing amber
            textLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 12));

            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web("#FFB703"));
            glow.setRadius(3.0);
            glow.setSpread(0.2);
            textLabel.setEffect(glow);

            rect.setStroke(Color.web("#FFB703"));
            rect.setStrokeWidth(1.5);
            cell.getChildren().add(textLabel);
        } else if (type == NodeType.DROP_ZONE) {
            dropZoneCounter++;
            String label = "DZ" + dropZoneCounter;
            stationLabels.put(cellId, label);
            Text textLabel = new Text(label);
            textLabel.setFill(Color.web("#00FF66")); // Glowing green
            textLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 12));

            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web("#00FF66"));
            glow.setRadius(3.0);
            glow.setSpread(0.2);
            textLabel.setEffect(glow);

            rect.setStroke(Color.web("#00FF66"));
            rect.setStrokeWidth(1.5);
            cell.getChildren().add(textLabel);
        }
    }

    public void placeRobotProgrammatic(int col, int row, double batteryLevel) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        String cellId = "N_" + col + "_" + row;

        routingService.generateGraphFromGrid(gridState);
        Graph graph = routingService.getWarehouseMap();
        Node targetNode = null;
        for (Node n : graph.getNodes()) {
            if (n.getId().equals(cellId)) {
                targetNode = n;
                break;
            }
        }
        if (targetNode == null) {
            targetNode = new Node(cellId, col, row, gridState[row][col]);
        }

        Robot robot = robotManagementService.registerRobot(targetNode);
        robot.setBatteryLevel(batteryLevel);

        // Render robot visual
        StackPane cell = cellGrid[row][col];
        for (javafx.scene.Node child : new java.util.ArrayList<>(cell.getChildren())) {
            if (child.getId() != null && child.getId().startsWith("robot-visual-")) {
                stopRobotVisualAnimations(child);
            }
        }
        cell.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("robot-visual-"));
        cell.getChildren().add(createRobotVisual(robot));
        cell.toFront();
    }

    public void placeObstacleProgrammatic(int col, int row) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        String cellId = "N_" + col + "_" + row;
        StackPane cell = cellGrid[row][col];
        cell.getChildren().removeIf(node -> node instanceof Text);
        stationLabels.remove(cellId);
        gridState[row][col] = NodeType.OBSTACLE;
        Rectangle rect = (Rectangle) cell.getChildren().get(0);
        rect.setFill(Color.web("#FF3333"));
        rect.setStroke(Color.web("#00FF66", 0.15));
        rect.setStrokeWidth(1.0);
    }

    public void loadDemoScenario() {
        // 1. Place Charging Stations CS1 at (1,13) and CS2 at (13,2)
        placeStationProgrammatic(1, 13, NodeType.CHARGING_STATION);
        placeStationProgrammatic(13, 2, NodeType.CHARGING_STATION);

        // 2. Place 12 Drop Zones scattered randomly
        placeStationProgrammatic(3, 4, NodeType.DROP_ZONE);
        placeStationProgrammatic(4, 10, NodeType.DROP_ZONE);
        placeStationProgrammatic(11, 3, NodeType.DROP_ZONE);
        placeStationProgrammatic(10, 11, NodeType.DROP_ZONE);
        placeStationProgrammatic(7, 2, NodeType.DROP_ZONE);
        placeStationProgrammatic(8, 12, NodeType.DROP_ZONE);
        placeStationProgrammatic(2, 8, NodeType.DROP_ZONE);
        placeStationProgrammatic(12, 7, NodeType.DROP_ZONE);
        placeStationProgrammatic(6, 6, NodeType.DROP_ZONE);
        placeStationProgrammatic(8, 7, NodeType.DROP_ZONE);
        placeStationProgrammatic(5, 13, NodeType.DROP_ZONE);
        placeStationProgrammatic(13, 10, NodeType.DROP_ZONE);

        // 3. Place 3 Robots, all initialized with 21.0% battery to trigger charging
        placeRobotProgrammatic(0, 0, 21.0);
        placeRobotProgrammatic(14, 0, 21.0);
        placeRobotProgrammatic(0, 14, 21.0);

        // 4. Place obstacles around DZ12 (13, 10): Left (12, 10), Top (13, 9), Bottom (13, 11)
        placeObstacleProgrammatic(12, 10);
        placeObstacleProgrammatic(13, 9);
        placeObstacleProgrammatic(13, 11);
    }

    public NodeType[][] getGridState() {
        return gridState;
    }

    public PlacementMode getCurrentPlacementMode() {
        return currentPlacementMode;
    }

    public void setCurrentPlacementMode(PlacementMode mode) {
        this.currentPlacementMode = mode;
    }

    public void startWorkflowSimulation(Robot robot, java.util.Queue<com.warehouse.model.domain.Task> taskQueue) {
        robotReservations.clear();
        executeNextTask(robot, taskQueue);
    }

    /**
     * Fans out task queues to each robot independently.
     * Each robot runs its own async animation chain concurrently —
     * they do not block each other.
     */
    public void startMultiRobotSimulation(java.util.Map<String, Robot> robotById,
            java.util.Map<String, java.util.Queue<com.warehouse.model.domain.Task>> robotQueues,
            RightSidebarView sidebar) {
        robotReservations.clear();
        // Count how many robots actually have tasks
        int[] remaining = { 0 };
        for (java.util.Queue<com.warehouse.model.domain.Task> q : robotQueues.values()) {
            if (!q.isEmpty())
                remaining[0]++;
        }
        if (remaining[0] == 0) {
            sidebar.updateKahnStatus("IDLE");
            return;
        }
        for (java.util.Map.Entry<String, java.util.Queue<com.warehouse.model.domain.Task>> entry : robotQueues
                .entrySet()) {
            Robot robot = robotById.get(entry.getKey());
            java.util.Queue<com.warehouse.model.domain.Task> queue = entry.getValue();
            if (robot == null || queue.isEmpty())
                continue;
            executeNextTaskMulti(robot, queue, sidebar, remaining);
        }
    }

    private void executeNextTaskMulti(Robot robot,
            java.util.Queue<com.warehouse.model.domain.Task> taskQueue,
            RightSidebarView sidebar,
            int[] remaining) {
        if (taskQueue.isEmpty()) {
            remaining[0]--;
            statusBar.setStatus(robot.getId() + " finished all assigned tasks.");
            if (remaining[0] <= 0) {
                statusBar.setStatus("All robots completed their tasks!");
                sidebar.updateKahnStatus("IDLE");
            }
            return;
        }
        com.warehouse.model.domain.Task nextTask = taskQueue.poll();
        executeTaskForRobot(robot, nextTask, taskQueue, sidebar, remaining);
    }

    private void executeTaskForRobot(Robot robot,
            com.warehouse.model.domain.Task nextTask,
            java.util.Queue<com.warehouse.model.domain.Task> taskQueue,
            RightSidebarView sidebar,
            int[] remaining) {
        String targetId = nextTask.getTargetNodeId();

        List<String> occupied = new java.util.ArrayList<>();
        for (Robot r : robotManagementService.getActiveFleet()) {
            if (!r.getId().equals(robot.getId()) && r.getCurrentNode() != null) {
                occupied.add(r.getCurrentNode().getId());
            }
        }
        routingService.generateGraphFromGrid(gridState, occupied);
        Graph graph = routingService.getWarehouseMap();

        Node destNode = null;
        for (Node n : graph.getNodes()) {
            if (n.getId().equals(targetId)) {
                destNode = n;
                break;
            }
        }

        if (destNode == null) {
            statusBar.setStatus(robot.getId() + ": skipping " + nextTask.getId() + " — target blocked.");
            executeNextTaskMulti(robot, taskQueue, sidebar, remaining);
            return;
        }

        Node startNode = robot.getCurrentNode();
        if (startNode == null) {
            statusBar.setStatus("Error: " + robot.getId() + " has no current location.");
            executeNextTaskMulti(robot, taskQueue, sidebar, remaining);
            return;
        }

        if (startNode.equals(destNode)) {
            StackPane visual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
            if (destNode.getNodeType() == NodeType.CHARGING_STATION) {
                robot.setBatteryLevel(100.0);
                updateRobotTelemetryHUD(robot, visual);
            }
            nextTask.setActive(false);
            nextTask.setProgress(1.0);
            rightSidebar.refreshCreatedTasksListView();
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(0.8));
            if (visual != null) {
                visual.getProperties().put("taskPause", p);
            }
            p.setOnFinished(ev -> executeNextTaskMulti(robot, taskQueue, sidebar, remaining));
            p.play();
            return;
        }

        List<Edge> path = routingService.calculateRoute(startNode, destNode);
        if (path == null || path.isEmpty()) {
            statusBar.setStatus(robot.getId() + ": no path for " + nextTask.getId());
            executeNextTaskMulti(robot, taskQueue, sidebar, remaining);
            return;
        }

        statusBar.setStatus(robot.getId() + " \u2192 " + nextTask.getName() + " (" + path.size() + " steps)");
        nextTask.setActive(true);
        nextTask.setProgress(0.0);
        robot.setActiveTaskId(nextTask.getId());
        rightSidebar.refreshCreatedTasksListView();
        highlightCalculatedRoute(robot.getId(), path);

        final Node finalDest = destNode;
        final com.warehouse.model.domain.Task finalTask = nextTask;

        executeNextPathStep(robot, path, 0, finalTask, taskQueue, false, () -> {
            setRobotMovingState(robot, false);
            robot.setCurrentNode(finalDest);
            robot.setActiveTaskId("None");
            activeRobotPaths.remove(robot.getId());
            redrawActiveGridLayers();
            StackPane visual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
            updateRobotTelemetryHUD(robot, visual);
            statusBar.setStatus(robot.getId() + " completed: " + finalTask.getName());
            finalTask.setActive(false);
            finalTask.setProgress(1.0);
            rightSidebar.refreshCreatedTasksListView();
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(0.8));
            if (visual != null) {
                visual.getProperties().put("taskPause", p);
            }
            p.setOnFinished(ev -> executeNextTaskMulti(robot, taskQueue, sidebar, remaining));
            p.play();
        }, () -> executeNextTaskMulti(robot, taskQueue, sidebar, remaining));
    }

    private void executeNextTask(Robot robot, java.util.Queue<com.warehouse.model.domain.Task> taskQueue) {
        if (taskQueue.isEmpty()) {
            statusBar.setStatus("All scheduled tasks completed successfully!");
            rightSidebar.updateKahnStatus("IDLE");
            return;
        }

        com.warehouse.model.domain.Task nextTask = taskQueue.poll();
        String targetId = nextTask.getTargetNodeId();

        // Rebuild full active graph using occupied nodes of other robots
        List<String> occupied = new java.util.ArrayList<>();
        for (Robot r : robotManagementService.getActiveFleet()) {
            if (!r.getId().equals(robot.getId()) && r.getCurrentNode() != null) {
                occupied.add(r.getCurrentNode().getId());
            }
        }
        routingService.generateGraphFromGrid(gridState, occupied);
        Graph graph = routingService.getWarehouseMap();

        Node destNode = null;
        for (Node n : graph.getNodes()) {
            if (n.getId().equals(targetId)) {
                destNode = n;
                break;
            }
        }

        if (destNode == null) {
            statusBar.setStatus("Skipping Task " + nextTask.getId() + ": Target location is blocked or invalid.");
            executeNextTask(robot, taskQueue);
            return;
        }

        Node startNode = robot.getCurrentNode();
        if (startNode == null) {
            statusBar.setStatus("Error: Robot has no current location.");
            return;
        }

        if (startNode.equals(destNode)) {
            statusBar.setStatus(
                    "Robot " + robot.getId() + " is already at " + destNode.getId() + " for " + nextTask.getName());
            StackPane visual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
            if (destNode.getNodeType() == NodeType.CHARGING_STATION
                    || gridState[destNode.getY()][destNode.getX()] == NodeType.CHARGING_STATION) {
                robot.setBatteryLevel(100.0);
                updateRobotTelemetryHUD(robot, visual);
                statusBar.setStatus("Robot " + robot.getId() + " recharged to 100% at " + destNode.getId());
            }
            nextTask.setActive(false);
            nextTask.setProgress(1.0);
            rightSidebar.refreshCreatedTasksListView();

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.0));
            if (visual != null) {
                visual.getProperties().put("taskPause", pause);
            }
            pause.setOnFinished(e -> executeNextTask(robot, taskQueue));
            pause.play();
            return;
        }

        List<Edge> path = routingService.calculateRoute(startNode, destNode);
        if (path == null || path.isEmpty()) {
            statusBar.setStatus("Error: No navigable path found for task " + nextTask.getId());
            executeNextTask(robot, taskQueue);
            return;
        }

        statusBar.setStatus("Executing Task: " + nextTask.getName() + " -> Routing to " + destNode.getId());

        // Set task active and draw path discovery
        nextTask.setActive(true);
        nextTask.setProgress(0.0);
        robot.setActiveTaskId(nextTask.getId());
        rightSidebar.refreshCreatedTasksListView();

        highlightCalculatedRoute(robot.getId(), path);

        final Node finalDest = destNode;
        final com.warehouse.model.domain.Task finalTask = nextTask;

        // Animate movement step-by-step
        executeNextPathStep(robot, path, 0, finalTask, taskQueue, false, () -> {
            setRobotMovingState(robot, false);
            robot.setCurrentNode(finalDest); // update position upon arrival
            robot.setActiveTaskId("None");
            activeRobotPaths.remove(robot.getId());
            redrawActiveGridLayers();

            StackPane visual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
            updateRobotTelemetryHUD(robot, visual);

            statusBar.setStatus("Task Completed: " + finalTask.getName() + " at " + finalDest.getId());

            finalTask.setActive(false);
            finalTask.setProgress(1.0);
            rightSidebar.refreshCreatedTasksListView();

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.0));
            if (visual != null) {
                visual.getProperties().put("taskPause", pause);
            }
            pause.setOnFinished(e -> executeNextTask(robot, taskQueue));
            pause.play();
        }, () -> executeNextTask(robot, taskQueue));
    }

    private void executeNextPathStep(Robot robot,
            List<Edge> path,
            int pathIndex,
            com.warehouse.model.domain.Task activeTask,
            java.util.Queue<com.warehouse.model.domain.Task> taskQueue,
            boolean isEmergencyCharging,
            Runnable onArrival,
            Runnable onResumeQueue) {
        if (pathIndex >= path.size()) {
            activeRobotPaths.remove(robot.getId());
            robotReservations.remove(robot.getId());
            redrawActiveGridLayers();
            onArrival.run();
            return;
        }

        StackPane robotVisual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
        if (robotVisual == null) {
            Edge firstEdge = path.get(pathIndex);
            Node srcNode = firstEdge.getSource();
            robotVisual = createRobotVisual(robot);
            cellGrid[srcNode.getY()][srcNode.getX()].getChildren().add(robotVisual);
        }

        // Before stepping onto a new tile, check if batteryLevel < 20.0 (Smart Check)
        if (!isEmergencyCharging && robot.getBatteryLevel() < 20.0) {
            statusBar.setStatus("Warning: Robot " + robot.getId() + " battery low ("
                    + String.format("%.1f", robot.getBatteryLevel()) + "%). Initiating Emergency Charging Override!");

            // Interrupt the active task and queue
            if (activeTask != null) {
                activeTask.setActive(false);
                if (taskQueue instanceof java.util.LinkedList) {
                    ((java.util.LinkedList<com.warehouse.model.domain.Task>) taskQueue).addFirst(activeTask);
                }
            }

            // Set active task ID to Charging
            robot.setActiveTaskId("CHARGING");
            robotReservations.remove(robot.getId());
            updateRobotTelemetryHUD(robot, robotVisual);
            rightSidebar.refreshCreatedTasksListView();

            // Find nearest charging station
            routingService.generateGraphFromGrid(gridState);
            Graph currentGraph = routingService.getWarehouseMap();
            List<Node> stations = new java.util.ArrayList<>();
            for (Node n : currentGraph.getNodes()) {
                if (n.getNodeType() == NodeType.CHARGING_STATION) {
                    stations.add(n);
                }
            }

            if (stations.isEmpty()) {
                statusBar.setStatus("Error: No charging station placed on grid! Continuing active task...");
                // Re-enable task if possible and continue
                if (activeTask != null) {
                    activeTask.setActive(true);
                    robot.setActiveTaskId(activeTask.getId());
                }
            } else {
                // Find shortest route to a station
                List<Edge> bestRoute = null;
                Node targetStation = null;
                for (Node station : stations) {
                    List<Edge> route = routingService.calculateRoute(robot.getCurrentNode(), station);
                    if (route != null && !route.isEmpty()) {
                        if (bestRoute == null || route.size() < bestRoute.size()) {
                            bestRoute = route;
                            targetStation = station;
                        }
                    }
                }

                if (bestRoute == null) {
                    statusBar.setStatus("Error: Charging stations are unreachable! Continuing active task...");
                    if (activeTask != null) {
                        activeTask.setActive(true);
                        robot.setActiveTaskId(activeTask.getId());
                    }
                } else {
                    final Node finalStation = targetStation;
                    final List<Edge> finalEmergencyRoute = bestRoute;

                    // Route to charging station
                    // Add emergency route path to map
                    activeRobotPaths.put(robot.getId(), finalEmergencyRoute);
                    redrawActiveGridLayers();

                    executeNextPathStep(robot, finalEmergencyRoute, 0, null, taskQueue, true, () -> {
                        setRobotMovingState(robot, false);
                        // Immediately set battery level to 100.0% when the robot lands on the charging
                        // station
                        robot.setBatteryLevel(100.0);
                        robot.setActiveTaskId("None");
                        activeRobotPaths.remove(robot.getId());
                        robotReservations.remove(robot.getId());
                        redrawActiveGridLayers();
                        StackPane visual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
                        updateRobotTelemetryHUD(robot, visual);

                        statusBar.setStatus("Robot " + robot.getId() + " arrived at Charging Station. Recharging...");

                        // Recharge transition (1.5 seconds)
                        javafx.animation.PauseTransition rechargeTimer = new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(1.5));
                        if (visual != null) {
                            visual.getProperties().put("rechargeTimer", rechargeTimer);
                        }
                        rechargeTimer.setOnFinished(evt -> {
                            statusBar.setStatus(
                                    "Robot " + robot.getId() + " fully recharged (100.0%). Resuming task queue.");
                            updateRobotTelemetryHUD(robot, visual);

                            // Resume queue
                            if (onResumeQueue != null) {
                                onResumeQueue.run();
                            }
                        });
                        rechargeTimer.play();
                    }, onResumeQueue);
                    return;
                }
            }
        }

        // Normal path step execution
        Edge edge = path.get(pathIndex);
        Node srcNode = edge.getSource();
        Node destNode = edge.getDestination();

        // Collision / occupancy / reservation check:
        boolean nextTileOccupied = false;
        for (Robot r : robotManagementService.getActiveFleet()) {
            if (!r.getId().equals(robot.getId())) {
                // Check if the other robot is currently at destNode
                if (r.getCurrentNode() != null && r.getCurrentNode().getX() == destNode.getX()
                        && r.getCurrentNode().getY() == destNode.getY()) {
                    nextTileOccupied = true;
                    break;
                }
                // Check if the other robot has reserved destNode
                String reservedNodeId = robotReservations.get(r.getId());
                if (reservedNodeId != null && reservedNodeId.equals(destNode.getId())) {
                    nextTileOccupied = true;
                    break;
                }
            }
        }

        if (nextTileOccupied) {
            Node finalDest = path.get(path.size() - 1).getDestination();

            // If the next tile is the final destination, we cannot bypass it with a detour.
            // Just wait for it to clear.
            if (destNode.getId().equals(finalDest.getId())) {
                setRobotMovingState(robot, false);
                statusBar.setStatus("Warning: Destination " + destNode.getId() + " is occupied. Robot " + robot.getId() + " is waiting...");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(500));
                if (robotVisual != null) {
                    robotVisual.getProperties().put("occupiedWaitPause", pause);
                }
                pause.setOnFinished(e -> {
                    setRobotMovingState(robot, true);
                    executeNextPathStep(robot, path, pathIndex, activeTask, taskQueue, isEmergencyCharging, onArrival,
                            onResumeQueue);
                });
                pause.play();
                return;
            }

            statusBar.setStatus("Recalculating detour for robot " + robot.getId() + " because next tile "
                    + destNode.getId() + " is occupied.");

            // Gather all occupied and reserved node IDs of other active robots
            List<String> occupied = new java.util.ArrayList<>();
            for (Robot r : robotManagementService.getActiveFleet()) {
                if (!r.getId().equals(robot.getId())) {
                    if (r.getCurrentNode() != null) {
                        occupied.add(r.getCurrentNode().getId());
                    }
                    String reservedNodeId = robotReservations.get(r.getId());
                    if (reservedNodeId != null) {
                        occupied.add(reservedNodeId);
                    }
                }
            }
            // Generate graph layout with coworker cells as penalty weight (100.0)
            routingService.generateGraphFromGrid(gridState, occupied);

            // Calculate detour from current node (robot's current position) to final destination
            List<Edge> detourPath = routingService.calculateRoute(robot.getCurrentNode(), finalDest);

            if (detourPath != null && !detourPath.isEmpty()) {
                // Check if the first step of the detour path is occupied
                Node nextDetourNode = detourPath.get(0).getDestination();
                boolean detourOccupied = false;
                for (Robot r : robotManagementService.getActiveFleet()) {
                    if (!r.getId().equals(robot.getId())) {
                        if (r.getCurrentNode() != null && r.getCurrentNode().getX() == nextDetourNode.getX()
                                && r.getCurrentNode().getY() == nextDetourNode.getY()) {
                            detourOccupied = true;
                            break;
                        }
                        String reserved = robotReservations.get(r.getId());
                        if (reserved != null && reserved.equals(nextDetourNode.getId())) {
                            detourOccupied = true;
                            break;
                        }
                    }
                }

                if (!detourOccupied) {
                    // Seamlessly update the remaining path steps
                    activeRobotPaths.put(robot.getId(), detourPath);
                    redrawActiveGridLayers();

                    // Recursively execute from step 0 of the detour path
                    executeNextPathStep(robot, detourPath, 0, activeTask, taskQueue, isEmergencyCharging, onArrival,
                            onResumeQueue);
                    return;
                }
            }

            // Fallback: If no detour is found or the detour path's first step is also occupied,
            // wait for a short moment and try the original path step again.
            setRobotMovingState(robot, false);
            statusBar.setStatus(
                    "Warning: No clear detour found for robot " + robot.getId() + ". Waiting...");
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(500));
            if (robotVisual != null) {
                robotVisual.getProperties().put("detourWaitPause", pause);
            }
            pause.setOnFinished(e -> {
                setRobotMovingState(robot, true);
                executeNextPathStep(robot, path, pathIndex, activeTask, taskQueue, isEmergencyCharging, onArrival,
                        onResumeQueue);
            });
            pause.play();
            return;
        }

        // If start of a path, set arms to moving (tilt back)
        if (pathIndex == 0) {
            setRobotMovingState(robot, true);
        }

        // Reserve the next tile
        robotReservations.put(robot.getId(), destNode.getId());

        // Deduct battery
        robot.setBatteryLevel(robot.getBatteryLevel() - 1.5);

        // Bring the source cell containing the robot visual to front so it renders on top during movement
        cellGrid[srcNode.getY()][srcNode.getX()].toFront();

        // Update telemetry HUD
        updateRobotTelemetryHUD(robot, robotVisual);

        // Update the active path in the map to show remaining path starting from this
        // step
        List<Edge> remainingPath = new java.util.ArrayList<>(path.subList(pathIndex, path.size()));
        activeRobotPaths.put(robot.getId(), remainingPath);
        redrawActiveGridLayers();

        // Move visual using TranslateTransition and RotateTransition in parallel
        int dx = destNode.getX() - srcNode.getX();
        int dy = destNode.getY() - srcNode.getY();
        double offset = CELL_SIZE + 1.0;
        double tx = dx * offset;
        double ty = dy * offset;

        // Calculate travel rotation target angle
        double targetAngle = -30.0;
        if (dx > 0) {
            targetAngle = 180.0; // Right
        } else if (dx < 0) {
            targetAngle = 0.0; // Left
        } else if (dy > 0) {
            targetAngle = -45.0; // Down
        } else if (dy < 0) {
            targetAngle = 100.0; // Up
        }

        double currentSpeed = this.rightSidebar.getSimulationSpeed();

        javafx.scene.transform.Rotate travelRotate = (javafx.scene.transform.Rotate) robotVisual.getProperties()
                .get("travelRotate");
        javafx.animation.Timeline rotTimeline = null;
        if (travelRotate != null) {
            rotTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(currentSpeed * 0.5),
                            new javafx.animation.KeyValue(travelRotate.angleProperty(), targetAngle,
                                    javafx.animation.Interpolator.EASE_BOTH)));
        }

        final StackPane finalVisual = robotVisual;
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(currentSpeed), finalVisual);
        tt.setFromX(0.0);
        tt.setFromY(0.0);
        tt.setToX(tx);
        tt.setToY(ty);

        Runnable onFinishedAction = () -> {
            // Reset translation first to prevent coordinate multiplication errors or visual
            // jumps
            finalVisual.setTranslateX(0.0);
            finalVisual.setTranslateY(0.0);

            // Reparent visual node
            cellGrid[srcNode.getY()][srcNode.getX()].getChildren().remove(finalVisual);

            // Clean up destination cell of any old robot-visual of this robot
            cellGrid[destNode.getY()][destNode.getX()].getChildren().removeIf(
                    n -> n.getId() != null && n.getId().equals("robot-visual-" + robot.getId()));
            cellGrid[destNode.getY()][destNode.getX()].getChildren().add(finalVisual);

            // Bring the destination cell containing the robot visual to front
            cellGrid[destNode.getY()][destNode.getX()].toFront();

            // Update model & clear reservation
            robot.setCurrentNode(destNode);
            robotReservations.remove(robot.getId());

            // If the robot moves onto a charging station, recharge battery to 100.0%
            if (destNode.getNodeType() == NodeType.CHARGING_STATION) {
                robot.setBatteryLevel(100.0);
                updateRobotTelemetryHUD(robot, finalVisual);
                statusBar.setStatus("Robot " + robot.getId() + " recharged to 100% at " + destNode.getId());
            }

            // Update task progress if active
            if (activeTask != null) {
                double progress = (double) (pathIndex + 1) / path.size();
                activeTask.setProgress(progress);
                rightSidebar.refreshCreatedTasksListView();
            }

            // Next step
            executeNextPathStep(robot, path, pathIndex + 1, activeTask, taskQueue, isEmergencyCharging, onArrival,
                    onResumeQueue);
        };

        if (rotTimeline != null) {
            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(rotTimeline, tt);
            if (robotVisual != null) {
                robotVisual.getProperties().put("activeMoveTransition", pt);
            }
            pt.setOnFinished(evt -> onFinishedAction.run());
            pt.play();
        } else {
            if (robotVisual != null) {
                robotVisual.getProperties().put("activeMoveTransition", tt);
            }
            tt.setOnFinished(evt -> onFinishedAction.run());
            tt.play();
        }
    }
}
