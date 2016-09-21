Uses genetic algorithm to turn randomly generated image into target image.

Cross over happens by randomly choosing some number of polygons from each parent solution, and for each pair of polygons, averaging the RGB values.

I have not been able to get anything very close to the target image. Past the current generations of 65,000 the image starts becoming one big polygon. The colors are also not correct, I realize now that might have to do with the way I crossover colors. Program takes about 5-7 minutes to run.