import sys

# Read file
with open('src/com/fpsgame/client/GamePanel.java', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

print(f"Total lines: {len(lines)}")

# Modification 1: Add HP initialization after line 1176 (after currentCharacterData assignment)
# Find the line with currentCharacterData
for i, line in enumerate(lines):
    if 'this.currentCharacterData = CharacterData.getById(selectedCharacter);' in line:
        print(f"Found currentCharacterData at line {i+1}")
        # Insert HP initialization after the blank line
        if i+2 < len(lines) and '// ' in lines[i+2]:
            # Insert before the comment
            lines.insert(i+2, '        \n')
            lines.insert(i+3, '        // HP 초기화 (중요: 캐릭터별 MaxHP 적용)\n')
            lines.insert(i+4, '        this.myMaxHP = (int) currentCharacterData.health;\n')
            lines.insert(i+5, '        this.myHP = this.myMaxHP;\n')
            print("✓ Inserted HP initialization")
        break

# Modification 2: Add GameConfig.saveCharacter() call
for i, line in enumerate(lines):
    if 'hasChangedCharacterInRound = true;' in line:
        print(f"Found hasChangedCharacterInRound at line {i+1}")
        # Insert after this line
        lines.insert(i+1, '                        \n')
        lines.insert(i+2, '                        // 변경된 캐릭터 저장\n')
        lines.insert(i+3, '                        GameConfig.saveCharacter(charId);\n')
        print("✓ Inserted GameConfig.saveCharacter() call")
        break

# Write back
with open('src/com/fpsgame/client/GamePanel.java', 'w', encoding='utf-8') as f:
    f.writelines(lines)

print("\n✓ Successfully updated GamePanel.java")
