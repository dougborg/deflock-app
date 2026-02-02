import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../services/localization_service.dart';

class LanguageSection extends StatefulWidget {
  const LanguageSection({super.key});

  @override
  State<LanguageSection> createState() => _LanguageSectionState();
}

class _LanguageSectionState extends State<LanguageSection> {
  String? _selectedLanguage;
  Map<String, String> _languageNames = {};

  @override
  void initState() {
    super.initState();
    _loadSelectedLanguage();
    _loadLanguageNames();
  }

  Future<void> _loadSelectedLanguage() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _selectedLanguage = prefs.getString('language_code');
    });
  }

  Future<void> _loadLanguageNames() async {
    final locService = LocalizationService.instance;
    final Map<String, String> names = {};
    
    for (String langCode in locService.availableLanguages) {
      names[langCode] = await locService.getLanguageDisplayName(langCode);
    }
    
    setState(() {
      _languageNames = names;
    });
  }

  Future<void> _setLanguage(String? languageCode) async {
    await LocalizationService.instance.setLanguage(languageCode);
    setState(() {
      _selectedLanguage = languageCode;
    });
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: LocalizationService.instance,
      builder: (context, child) {
        final locService = LocalizationService.instance;
        
        return RadioGroup<String?>(
          groupValue: _selectedLanguage,
          onChanged: _setLanguage,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // System Default option
              RadioListTile<String?>(
                title: Text(locService.t('settings.systemDefault')),
                value: null,
              ),
              // English always appears second (if available)
              if (locService.availableLanguages.contains('en'))
                RadioListTile<String>(
                  title: Text(_languageNames['en'] ?? 'English'),
                  value: 'en',
                ),
              // Other language options (excluding English since it's already shown)
              ...locService.availableLanguages
                  .where((langCode) => langCode != 'en')
                  .map((langCode) =>
                RadioListTile<String>(
                  title: Text(_languageNames[langCode] ?? langCode.toUpperCase()),
                  value: langCode,
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}