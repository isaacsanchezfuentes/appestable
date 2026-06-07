import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:flutter_app/services/auth_service.dart';
import 'package:flutter_app/services/personas_provider.dart';
import 'package:flutter_app/main.dart';

void main() {
  testWidgets('App muestra pantalla de login sin sesión', (WidgetTester tester) async {
    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => AuthService()),
          ChangeNotifierProvider(create: (_) => PersonasProvider()),
        ],
        child: const MyApp(),
      ),
    );

    expect(find.text('Inicia sesión para acceder a tus viajes'), findsOneWidget);
    expect(find.text('INICIAR SESIÓN'), findsOneWidget);
  });
}