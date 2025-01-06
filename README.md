# Minecraft Plugin

This is a Minecraft plugin designed for PaperMC servers, offering integration with OpenAI.

## Requirements

- **Server Software**: PaperMC 1.21.3
- **Plugin Version**: English version
- **API Key**: You need to provide your own OpenAI API key in the `config.yml` file for the plugin to work.

## Setup

1. Download the plugin and place it in the `plugins` folder of your PaperMC server.
2. Start the server to generate the default configuration file (`config.yml`).
3. Open the `config.yml` file and enter your OpenAI API key in the designated field.
4. Restart the server to apply the changes.

## Configuration (`config.yml`)

Make sure to add your OpenAI API key in the `config.yml` file under the appropriate section.

```yml
openai-api-key: "your-api-key-here"
```
## License
This project is subject to multiple licenses:

1. **Core Plugin Code**:  
   The codebase, excluding explicitly marked educational components, is licensed under **GNU General Public License v3.0 (GPLv3)**. You are free to use, modify, and distribute the code under the terms of GPLv3.
2. **Educational Components** (algorithms, chat messages, and dynamic logic related to educational content):  
   These portions are licensed under **Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)**. You may share and adapt these elements for non-commercial purposes with proper attribution.

For more details, see the LICENSE file.
