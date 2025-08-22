function visualizeParticles()
    %% Configuration
    SENSOR_POSITIONS = [0, 1500; 1500, 3000]; % [x, y] coordinates of sensors
    SENSOR_MAX_RANGE = [4000, 4000]; % Maximum sensor ranges for circle visualization
    STEP_SIZE = 10; % Time Step increment for viewer
    
    %% Load particle data
    try
        opts = detectImportOptions('particle_states.csv', 'NumHeaderLines', 0, 'VariableNamingRule', 'preserve');
        data = readtable('particle_states.csv', opts);
        fprintf('Particle data loaded successfully. Size: %d x %d\n', height(data), width(data));
    catch ME
        fprintf('Error loading particle CSV: %s\n', ME.message);
        return;
    end
    
    %% Load sensor data
    sensorData = [];
    try
        sensorOpts = detectImportOptions('sensor_data.csv', 'NumHeaderLines', 0, 'VariableNamingRule', 'preserve');
        sensorData = readtable('sensor_data.csv', sensorOpts);
        fprintf('Sensor data loaded successfully. Size: %d x %d\n', height(sensorData), width(sensorData));
    catch ME
        fprintf('Warning: Could not load sensor_data.csv: %s\n', ME.message);
        fprintf('Bearing cones will not be displayed.\n');
    end
    
    timeSteps = unique(data.TimeStep);
    numTimeSteps = length(timeSteps);

    % Initialize visualization
    currentTimeIndex = 0;
    figHandle = figure('Position', [0, 0, 1000, 600], ...
                      'Name', 'Interactive Particle Filter Visualization with Bearing Cones', ...
                      'KeyPressFcn', @keyPressCallback, ...
                      'CloseRequestFcn', @closeCallback);

    % Calculate global weight statistics for consistent color scaling
    globalMinWeight = min(data.Weight);
    globalMaxWeight = max(data.Weight);
    fprintf('Weight range: %.2e to %.2e\n', globalMinWeight, globalMaxWeight);

    
    updatePlot();
    
    fprintf('\n==== CONTROLS ====\n');
    fprintf('RIGHT ARROW:  Forward 10 time steps\n');
    fprintf('LEFT ARROW:   Backward 10 time steps\n');
    fprintf('ESC:          Quit\n');
    fprintf('================\n\n');
    
    % Keep the figure active
    uiwait(figHandle);
    
    %% Nested functions
    function updatePlot()
        if currentTimeIndex > numTimeSteps
            currentTimeIndex = numTimeSteps;
        elseif currentTimeIndex < 1
            currentTimeIndex = 1;
        end
        
        currentTime = timeSteps(currentTimeIndex);
        
        % Get data for current time step
        currentData = data(data.TimeStep == currentTime, :);
        
        if isempty(currentData)
            fprintf('No data found for time step %d\n', currentTime);
            return;
        end
        
        clf;
        hold on;
        
        % Plot sensor ranges (circles)
        for i = 1:size(SENSOR_POSITIONS, 1)
            theta = linspace(0, 2*pi, 100);
            circleX = SENSOR_POSITIONS(i, 1) + SENSOR_MAX_RANGE(i) * cos(theta);
            circleY = SENSOR_POSITIONS(i, 2) + SENSOR_MAX_RANGE(i) * sin(theta);
            
            fill(circleX, circleY, [0.8, 0.9, 1], 'FaceAlpha', 0.15, 'EdgeColor', [0.5, 0.5, 0.5], 'LineWidth', 1, 'LineStyle', ':');
        end
        
        % Plot bearing likelihood cones if sensor data is available
        if ~isempty(sensorData)
            currentSensorData = sensorData(sensorData.TimeStep == currentTime, :);
            
            for i = 1:height(currentSensorData)
                sensorX = currentSensorData.SensorX(i);
                sensorY = currentSensorData.SensorY(i);
                bearing = currentSensorData.ObservedBearing(i);
                stdDev = currentSensorData.BearingStdDev(i);
                
                plotBearingCone(sensorX, sensorY, bearing, stdDev, SENSOR_MAX_RANGE(i));
            end
        end
        
        if height(currentData) > 1
            scatterHandle = scatter(currentData.X, currentData.Y, 60, currentData.Weight, 'filled', ...
                   'MarkerFaceAlpha', 0.8, 'MarkerEdgeColor', 'none');
            clim([globalMinWeight, globalMaxWeight]);

            % logWeights = log10(max(currentData.Weight, 1e-15));
            % scatterHandle = scatter(currentData.X, currentData.Y, 60, logWeights, 'filled', ...
            %        'MarkerFaceAlpha', 0.8, 'MarkerEdgeColor', 'none');
            % clim([log10(max(globalMinWeight, 1e-15)), log10(globalMaxWeight)]);

            colormap('hot');
            cb = colorbar;
            cb.Label.String = 'Particle Weight';
            % cb.Label.String = 'Particle Weight (Log_{10})';

        end
        
        % Plot true position
        truePos = plot(currentData.TrueX(1), currentData.TrueY(1), 'bo', 'MarkerSize', 8, ...
             'MarkerFaceColor', 'red', 'LineWidth', 1.5);
        
        % Plot estimated position
        estPos = plot(currentData.EstimateX(1), currentData.EstimateY(1), 'go', 'MarkerSize', 8, ...
             'MarkerFaceColor', 'green', 'LineWidth', 1.5);
        
        % Plot sensor positions
        scatter(SENSOR_POSITIONS(:, 1), SENSOR_POSITIONS(:, 2), 100, 'k', 's', ...
               'filled', 'MarkerEdgeColor', 'white', 'LineWidth', 2);
        
        % Add sensor labels
        for i = 1:size(SENSOR_POSITIONS, 1)
            text(SENSOR_POSITIONS(i, 1) + 2, SENSOR_POSITIONS(i, 2) + 2, ...
                 sprintf('S%d', i), 'FontSize', 10, 'FontWeight', 'bold', ...
                 'BackgroundColor', 'white', 'EdgeColor', 'black');
        end
        
        xlabel('X Position (m)', 'FontSize', 12);
        ylabel('Y Position (m)', 'FontSize', 12);
        
        title(sprintf('Particle Filter - Time Step %d/%d', currentTimeIndex, numTimeSteps), ...
              'FontSize', 14, 'FontWeight', 'bold');
        
        legendHandles = [];
        legendLabels = {};
        
        legendHandles(end+1) = scatterHandle;
        legendLabels{end+1} = 'Particles';
        
        % True position
        legendHandles(end+1) = truePos;
        legendLabels{end+1} = 'True Position';
        
        % Estimated position
        legendHandles(end+1) = estPos;
        legendLabels{end+1} = 'Estimate';
        
        % Add bearing cone legend if we have sensor data
        if ~isempty(sensorData) && ~isempty(currentSensorData)
            % Create dummy line for legend
            bearingLine = plot(NaN, NaN, 'r-', 'LineWidth', 2);
            legendHandles(end+1) = bearingLine;
            legendLabels{end+1} = 'Bearing Cones';
        end
        
        legend(legendHandles, legendLabels, 'Location', 'northeast', 'FontSize', 8);

        grid on;
        ax = gca;
        % padding from left/bottom, total width left/bottom
        ax.Position = [0.15, 0.08, 0.65, 0.85]; 
        
        axis normal;
        allX = [currentData.X; currentData.TrueX(1); currentData.EstimateX(1); SENSOR_POSITIONS(:, 1)];
        allY = [currentData.Y; currentData.TrueY(1); currentData.EstimateY(1); SENSOR_POSITIONS(:, 2)];
        
        % margin for axes around lowest/highest x/y values
        margin = 1500; 
        xlimits = [min(allX) - margin, max(allX) + margin];
        ylimits = [min(allY) - margin, max(allY) + margin];
        
        xlim(xlimits);
        ylim(ylimits);
        
        drawnow;
    end
    
    function plotBearingCone(sensorX, sensorY, bearing, stdDev, maxRange)
        sigma2 = 2 * stdDev; % 2-sigma bounds
        
        bearing1 = bearing - sigma2;
        bearing2 = bearing + sigma2;
        
        range = maxRange * 0.9;
        
        x1_end = sensorX + range * cos(bearing1);
        y1_end = sensorY + range * sin(bearing1);
        
        x2_end = sensorX + range * cos(bearing2);
        y2_end = sensorY + range * sin(bearing2);
        
        % Plot the cone edges
        plot([sensorX, x1_end], [sensorY, y1_end], 'r-', 'LineWidth', 2, 'Color', [1, 0, 0, 0.7]);
        plot([sensorX, x2_end], [sensorY, y2_end], 'r-', 'LineWidth', 2, 'Color', [1, 0, 0, 0.7]);
        
        % center line
        x_center = sensorX + range * cos(bearing);
        y_center = sensorY + range * sin(bearing);
        plot([sensorX, x_center], [sensorY, y_center], 'r--', 'LineWidth', 1, 'Color', [1, 0, 0, 0.5]);
    end
    
    function keyPressCallback(~, event)
        switch lower(event.Key)
            case {'rightarrow'}
                % Forward 10 steps
                currentTimeIndex = currentTimeIndex + STEP_SIZE;
                updatePlot();
                
            case 'leftarrow'
                % Backward 10 steps
                currentTimeIndex = currentTimeIndex - STEP_SIZE;
                updatePlot();
                
            case {'escape'}
                % Quit
                fprintf('Exiting visualization\n');
                close(figHandle);
                
            otherwise
                % Display help for unknown keys
                fprintf('Unknown key: %s\n', event.Key);
                fprintf('Use RIGHT ARROW (forward), LEFT ARROW (back), ESC (quit)\n');
        end
    end
    
    function closeCallback(~, ~)
        delete(figHandle);
    end
end