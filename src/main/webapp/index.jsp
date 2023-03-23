<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Resize Image</title>
    </head>
    <body>
        <h1>ImageMagick Resize n&apos; Reformat</h1>
        <div id="version">Version: ${initParam.releaseNumber} (${initParam.releaseDate})</div>           
        <form method="post" action="convert" enctype="multipart/form-data" accept="image/*">
            <dl>
                <dt>
                <label for="file">Image File: </label>
                </dt>
                <dd>
                    <input id="file" type="file" name="file" accept="image/*"/>
                </dd>
                <dt>
                <label for="width">Width: </label>
                </dt>
                <dd>
                    <input id="width" name="width" type="text"/>
                </dd>
                <dt>
                <label for="height">Height: </label>
                </dt>
                <dd>
                    <input id="height" name="height" type="text"/>
                </dd>      
                <dt>
                <label for="allow-stretching">Allow Stretching: </label>
                </dt>
                <dd>
                    <input id="allow-stretching" name="allow-stretching" type="checkbox"/>
                </dd>
                <dt>
                <label for="ignore-aspect-ratio">Ignore Aspect Ratio: </label>
                </dt>
                <dd>
                    <input id="ignore-aspect-ratio" name="ignore-aspect-ratio" type="checkbox"/>
                </dd>
                <dt>
                <label for="output-data-uri">Output Data URI: </label>
                </dt>
                <dd>
                    <input id="output-data-uri" name="output-data-uri" type="checkbox"/>
                </dd>    
                <label for="force-png">Format as PNG (auto-orient JPEG EXIF): </label>
                </dt>
                <dd>
                    <input id="force-png" name="force-png" type="checkbox"/>
                </dd>                 
            </dl>
            <input type="submit" value="Resize"/>
        </form>
    </body>
</html>
