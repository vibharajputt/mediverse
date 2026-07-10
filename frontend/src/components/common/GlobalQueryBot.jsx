import { useState, useEffect, useRef } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  FiMessageSquare, 
  FiX, 
  FiSend, 
  FiMic, 
  FiMicOff, 
  FiCpu, 
  FiRefreshCw, 
  FiHelpCircle 
} from 'react-icons/fi';
import { aiAPI } from '../../services/api';
import { getOfflineAiResponse } from '../../services/offlineAi';
import toast from 'react-hot-toast';
import aiBotIcon from '../../assets/ai-bot-icon.png';
import './GlobalQueryBot.css';

const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

export default function GlobalQueryBot() {
  const location = useLocation();
  const [chatOpen, setChatOpen] = useState(false);
  const [chatMessage, setChatMessage] = useState('');
  const [chatSessionId, setChatSessionId] = useState(null);
  const [chatHistory, setChatHistory] = useState([
    {
      sender: 'ai',
      text: '👋 **Welcome to MedAstraX!** I am your 24/7 Platform Assistant.\n\nI can help you with:\n- 📅 [Booking an appointment](/dashboard) with a doctor\n- 📝 [Creating an account](/signup) or [logging in](/login)\n- 💊 [Buying & ordering medicines](/my-prescriptions)\n- 🎙️ Using AI clinical tools or diagnostic bookings\n- 🩺 General health and wellness questions\n\n*How can I help you today? You can type your query or click the microphone button next to me to ask with your voice!*'
    }
  ]);
  const [sendingChat, setSendingChat] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [recognition, setRecognition] = useState(null);
  const chatBodyRef = useRef(null);

  const isPatientDashboard = location.pathname.includes('/dashboard') || 
                             location.pathname.includes('/dash') || 
                             location.pathname.includes('/care-plan') || 
                             location.pathname.includes('/my-bookings') || 
                             location.pathname.includes('/my-prescriptions') || 
                             location.pathname.includes('/pharmacy-order') || 
                             location.pathname.includes('/diagnostic-order') || 
                             location.pathname.includes('/consultation-room') || 
                             location.pathname.includes('/emergency');

  const quickTags = [
    { label: '📅 Book Appointment', query: 'How do I book a doctor appointment on the platform?' },
    { label: '📝 Sign Up Guide', query: 'How can I register an account as a patient or doctor?' },
    { label: '💊 Buy Medicines', query: 'How can I order medicines online using my prescriptions?' },
    { label: '🏆 Earn Rewards', query: 'How does the EXP checklist and streak rewards program work?' }
  ];

  // Initialize Speech Recognition
  useEffect(() => {
    if (!SpeechRecognition) return;
    const rec = new SpeechRecognition();
    rec.continuous = false; // Stop listening once user pauses speaking
    rec.interimResults = false;
    rec.lang = 'en-US';

    rec.onstart = () => {
      setIsListening(true);
      toast.success('🎙️ Voice assistant listening... Speak now!', { id: 'voice-active' });
    };

    rec.onresult = (event) => {
      const resultText = event.results[0][0].transcript;
      if (resultText && resultText.trim()) {
        handleSendVoiceQuery(resultText);
      }
    };

    rec.onerror = (event) => {
      console.error('Speech recognition error:', event.error);
      if (event.error === 'not-allowed') {
        toast.error('Microphone permission denied! Please allow access in browser settings.', { id: 'voice-active' });
      } else {
        toast.error('Voice input error. Please try again.', { id: 'voice-active' });
      }
      setIsListening(false);
    };

    rec.onend = () => {
      setIsListening(false);
    };

    setRecognition(rec);
  }, []);

  // Scroll to bottom of chat
  useEffect(() => {
    if (chatBodyRef.current) {
      chatBodyRef.current.scrollTop = chatBodyRef.current.scrollHeight;
    }
  }, [chatHistory, sendingChat, chatOpen]);

  const handleToggleListening = () => {
    if (!SpeechRecognition) {
      toast.error('Speech recognition is not supported in this browser. Please use Chrome or Edge.');
      return;
    }

    if (isListening) {
      recognition.stop();
    } else {
      try {
        recognition.start();
      } catch (err) {
        console.error('Failed to start speech recognition:', err);
      }
    }
  };

  const handleSendVoiceQuery = (text) => {
    toast.success(`Captured: "${text}"`, { icon: '🎙️', id: 'voice-captured' });
    setChatOpen(true);
    handleSendChat(text);
  };

  const handleSendChat = async (textToSend) => {
    const msg = textToSend || chatMessage;
    if (!msg.trim() || sendingChat) return;

    setChatHistory(prev => [...prev, { sender: 'user', text: msg }]);
    if (!textToSend) setChatMessage('');
    setSendingChat(true);

    if (!navigator.onLine) {
      setTimeout(async () => {
        try {
          const reply = await getOfflineAiResponse(msg);
          setChatHistory(prev => [...prev, { sender: 'ai', text: reply }]);
        } catch (err) {
          console.error(err);
          setChatHistory(prev => [...prev, { sender: 'ai', text: '⚠️ **Error:** Failed to compute offline reply.' }]);
        } finally {
          setSendingChat(false);
        }
      }, 500);
      return;
    }

    try {
      const res = await aiAPI.queryChat(msg, chatSessionId);
      const reply = res.data.reply || 'Sorry, I couldn\'t formulate a reply. Please try again.';
      if (res.data.sessionId) {
        setChatSessionId(res.data.sessionId);
      }
      setChatHistory(prev => [...prev, { sender: 'ai', text: reply }]);
    } catch (err) {
      console.error(err);
      setChatHistory(prev => [...prev, { sender: 'ai', text: '⚠️ **Connection Error:** Could not connect to Astra. Please make sure the backend server is running and try again.' }]);
    } finally {
      setSendingChat(false);
    }
  };

  const handleResetChat = async () => {
    try {
      const res = await aiAPI.resetQueryChat(chatSessionId);
      if (res.data.sessionId) {
        setChatSessionId(res.data.sessionId);
      }
      toast.success('Chat history cleared! Fresh session started.');
    } catch (err) {
      console.error('Reset failed', err);
    }
    setChatHistory([
      {
        sender: 'ai',
        text: '👋 **Session reset!** How can I assist you with MedAstraX platform queries or wellness support?'
      }
    ]);
  };

  // Helper to parse markdown and convert relative links to React Router Link components
  const parseMarkdown = (text) => {
    if (!text) return '';
    const lines = text.split('\n');
    return lines.map((line, index) => {
      let content = line;
      content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      content = content.replace(/\*(.*?)\*/g, '<em>$1</em>');

      const linkRegex = /\[(.*?)\]\((.*?)\)/g;
      let match;
      let lastIndex = 0;
      const parts = [];

      while ((match = linkRegex.exec(content)) !== null) {
        const [fullMatch, linkText, linkUrl] = match;
        const matchIndex = match.index;

        if (matchIndex > lastIndex) {
          parts.push(
            <span 
              key={`text-${lastIndex}`} 
              dangerouslySetInnerHTML={{ __html: content.substring(lastIndex, matchIndex) }} 
            />
          );
        }

        if (linkUrl.startsWith('/')) {
          parts.push(
            <Link 
              key={`link-${matchIndex}`} 
              to={linkUrl} 
              onClick={() => setChatOpen(false)} // Close bot panel on link click for seamless flow
              className="chat-embedded-link"
            >
              {linkText}
            </Link>
          );
        } else {
          parts.push(
            <a 
              key={`extlink-${matchIndex}`} 
              href={linkUrl} 
              target="_blank" 
              rel="noopener noreferrer" 
              className="chat-embedded-link"
            >
              {linkText}
            </a>
          );
        }
        lastIndex = linkRegex.lastIndex;
      }

      if (lastIndex < content.length) {
        parts.push(
          <span 
            key={`text-${lastIndex}`} 
            dangerouslySetInnerHTML={{ __html: content.substring(lastIndex) }} 
          />
        );
      }

      if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
        return (
          <li key={index} className="chat-li">
            {parts.length > 0 ? parts : <span dangerouslySetInnerHTML={{ __html: content.trim().substring(2) }} />}
          </li>
        );
      }

      if (/^\d+\.\s/.test(line.trim())) {
        return (
          <li key={index} className="chat-li-decimal">
            {parts.length > 0 ? parts : <span dangerouslySetInnerHTML={{ __html: content.trim().replace(/^\d+\.\s/, '') }} />}
          </li>
        );
      }

      if (line.trim() === '') {
        return <div key={index} style={{ height: '8px' }} />;
      }

      return (
        <p key={index} className="chat-p">
          {parts.length > 0 ? parts : <span dangerouslySetInnerHTML={{ __html: content }} />}
        </p>
      );
    });
  };

  return (
    <div className={`global-query-bot-container ${isPatientDashboard ? 'dashboard-shifted' : ''}`}>
      
      {/* Voice Status Alert */}
      {isListening && (
        <div className="voice-listening-toast">
          <div className="mic-pulse-ring"></div>
          <span>🎙️ Listening to your query...</span>
        </div>
      )}

      {/* Floating Buttons: Mic & Chat Trigger */}
      <div className="global-bot-fab-group">
        
        {/* Voice Assistant Button */}
        <button 
          className={`global-mic-fab ${isListening ? 'listening' : ''}`}
          onClick={handleToggleListening}
          title={isListening ? "Stop Voice Input" : "Ask with AI Voice Assistant"}
        >
          {isListening ? (
            <FiMicOff size={20} className="mic-icon-off" />
          ) : (
            <FiMic size={20} className="mic-icon-on" />
          )}
          {isListening && (
            <div className="mic-waves">
              <span className="wave-bar"></span>
              <span className="wave-bar"></span>
              <span className="wave-bar"></span>
            </div>
          )}
        </button>

        {/* Chatbot Toggle Button */}
        <button 
          className={`global-chat-fab ${chatOpen ? 'open' : ''}`} 
          onClick={() => setChatOpen(!chatOpen)}
          title="MedAstraX Platform Assistant"
        >
          {chatOpen ? (
            <FiX size={22} />
          ) : (
            <div className="chat-fab-inner">
              <img src={aiBotIcon} alt="AI Helper" className="fab-bot-img" />
              <span className="fab-glow-effect"></span>
            </div>
          )}
        </button>
      </div>

      {/* Chat Interface Panel Overlay */}
      <AnimatePresence>
        {chatOpen && (
          <motion.div 
            className="global-chat-panel"
            initial={{ opacity: 0, y: 40, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 40, scale: 0.96 }}
            transition={{ type: 'spring', damping: 25, stiffness: 350 }}
          >
            {/* Panel Header */}
            <div className="global-chat-header">
              <div className="header-info">
                <div className="header-avatar">
                  <img src={aiBotIcon} alt="Astra" className="header-avatar-img" />
                </div>
                <div className="header-text">
                  <span className="header-title">Astra</span>
                  {!navigator.onLine ? (
                    <span className="header-subtitle" style={{ color: '#F59E0B', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <span className="offline-indicator-dot" style={{ backgroundColor: '#F59E0B', boxShadow: '0 0 6px #F59E0B' }}></span>
                      Offline Mode (TF.js)
                    </span>
                  ) : (
                    <span className="header-subtitle">
                      <span className="online-indicator-dot"></span>
                      24/7 Platform Guide
                    </span>
                  )}
                </div>
              </div>
              
              <div className="header-actions">
                <button
                  onClick={handleResetChat}
                  title="Reset Conversation"
                  className="header-btn-reset"
                >
                  <FiRefreshCw size={14} />
                </button>
                <button 
                  onClick={() => setChatOpen(false)} 
                  className="header-btn-close"
                >
                  <FiX size={18} />
                </button>
              </div>
            </div>

            {/* Chat History Area */}
            <div className="global-chat-body" ref={chatBodyRef}>
              {chatHistory.map((chat, i) => (
                <div key={i} className={`global-chat-message ${chat.sender}`}>
                  <span className="message-sender-name">
                    {chat.sender === 'user' ? 'You' : 'Astra'}
                  </span>
                  <div className="message-bubble">
                    {chat.sender === 'ai' ? parseMarkdown(chat.text) : chat.text}
                  </div>
                </div>
              ))}
              
              {sendingChat && (
                <div className="global-chat-message ai">
                  <span className="message-sender-name">Astra</span>
                  <div className="message-bubble typing">
                    <FiCpu className="typing-spinner" />
                    <span>Thinking...</span>
                  </div>
                </div>
              )}
            </div>

            {/* Suggested Tags Area */}
            <div className="global-chat-suggestions">
              {quickTags.map((tag, i) => (
                <button 
                  key={i} 
                  className="suggestion-pill" 
                  onClick={() => handleSendChat(tag.query)}
                >
                  <FiHelpCircle size={12} className="pill-icon" />
                  <span>{tag.label}</span>
                </button>
              ))}
            </div>

            {/* Chat Input form */}
            <form 
              onSubmit={(e) => { e.preventDefault(); handleSendChat(); }} 
              className="global-chat-input-area"
            >
              <input 
                type="text" 
                className="global-chat-input-field" 
                placeholder="Ask how to book, sign up, buy..." 
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
                disabled={sendingChat}
              />
              <button 
                type="submit" 
                className="global-chat-send-btn" 
                disabled={sendingChat || !chatMessage.trim()}
              >
                <FiSend size={16} />
              </button>
            </form>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
